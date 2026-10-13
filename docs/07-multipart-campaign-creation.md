# Backend-Owned Campaign Creation With Files

This supersedes the browser-orchestrated creation flow in the earlier upload notes. The backend now receives campaign details and both files in one request, uploads them to S3, and saves the resulting URIs in MySQL. Existing individual asset-upload routes remain available for replacement files when editing.

## Request

`POST /api/v1/users/{userId}/campaigns`, Authorization: Bearer token, Content-Type: multipart/form-data.

| Part | Type | Content |
| --- | --- | --- |
| `campaign` | application/json | Name, subject, recipient count, sender account ID |
| `template` | File | HTML/HTM/TXT, up to 1 MiB |
| `recipients` | File | CSV, up to 25 MiB |

The JSON part is:

```json
{
  "campaignName": "October launch",
  "subject": "Hello from Queueify",
  "totalEmails": 100,
  "senderEmailAccountId": 5
}
```

The client sends no `csvUrl` or `templateUrl`. In Postman select Body > form-data, add the JSON as a Text part named `campaign` and set that part's Content-Type to application/json, then add the two File parts. Let Postman set the outer multipart boundary.

The existing application/json creation route is retained for compatibility with clients that already supply storage references. Its contract is separate from multipart creation. The total multipart request limit is now 27 MiB to accommodate a maximum-size template and CSV plus metadata/boundaries.

## Backend Flow

`CampaignFileController` verifies the authenticated owner and validates the JSON part. `CampaignFileCreationService` checks the active sender and validates both files before writing any object. `CampaignAssetUploadService` and `S3StorageService` upload the files with unique owner-scoped keys.

After uploading, a short database transaction calls the existing campaign creation logic using the URIs returned by S3. This persists `campaigns.csv_url` and `campaigns.template_url`, creates a DRAFT, and returns 201 with the saved campaign response. S3 network calls occur before the database transaction begins on the normal HTTP path.

## Failure Handling

- If the second upload fails, attempt to remove the successfully uploaded first object.
- If the database transaction cannot start, attempt to remove both objects.
- If the transaction definitely rolls back, attempt to remove both objects using transaction synchronization.
- If commit outcome is unknown, retain both objects and log the need for reconciliation: deleting them could break a campaign that actually committed.
- Cleanup failures are logged and do not hide the original error or prevent an attempt to clean up the other file.

This is best-effort compensation, not an atomic transaction across MySQL and S3. Crashes and lost responses can still leave unreferenced objects or an uncertain create outcome. New campaign creation does not have an idempotency key; retries after a lost successful response can create duplicates. KAN-33's idempotent start action is a different operation.

## Frontend and Verification

The frontend sends one FormData request for creation and retains its selected files on failure. Retry resubmits both files. Edit/replacement retains the earlier individual-upload behavior.

Verification completed:

- Backend regression tests, including storage, validation, multipart binding, owner authorization, cleanup, and ambiguous-commit behavior.
- 14 Playwright browser workflow tests, frontend lint, and production build.
- An actual MySQL-backed multipart persistence test with mocked S3: inserted a draft, flushed/cleared the persistence context, reloaded it, and verified both server-generated S3 URI columns. Test records were rolled back afterward.

Run the database check explicitly with local MySQL available:

```bash
QUEUEIFY_MYSQL_INTEGRATION_TEST=true ./mvnw -q -Dtest=CampaignFilePersistenceIntegrationTest test
```

It is skipped in ordinary test runs. Its outer test transaction verifies SQL persistence after flush/reload, not a separately committed production HTTP request. The transaction failure tests use a controlled transaction manager. Live AWS upload still requires credentials available to the backend process; the new live S3 path has not been verified in the agent environment.
