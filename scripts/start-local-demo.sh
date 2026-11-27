#!/usr/bin/env bash
set -euo pipefail
config="${1:-$HOME/.config/queueify/demo.env}"
[[ -f "$config" ]] || { echo "Configure $config first."; exit 1; }
set -a
source "$config"
set +a
for name in QUEUEIFY_DEMO_ID QUEUEIFY_DEMO_DB_PASSWORD QUEUEIFY_CREDENTIAL_ENCRYPTION_KEY QUEUEIFY_INTERNAL_TASK_TOKEN QUEUEIFY_INTERNAL_CAMPAIGN_TOKEN QUEUEIFY_S3_BUCKET AWS_REGION; do
  [[ -n "${!name:-}" ]] || { echo "Missing $name in the private demo configuration."; exit 1; }
done
if [[ -z "${AWS_PROFILE:-}" && ( -z "${AWS_ACCESS_KEY_ID:-}" || -z "${AWS_SECRET_ACCESS_KEY:-}" ) ]]; then
  echo "Configure AWS_PROFILE or AWS access credentials locally; no values are printed."; exit 1
fi
[[ -n "${AWS_PROFILE:-}" ]] || unset AWS_PROFILE
[[ -n "${AWS_SESSION_TOKEN:-}" ]] || unset AWS_SESSION_TOKEN
unset QUEUEIFY_DEMO_GMAIL_APP_PASSWORD QUEUEIFY_DEMO_PASSWORD
campaign="$(cd "$(dirname "$0")/.." && pwd)"
processor="${PROCESSOR_DIR:-$(dirname "$campaign")/queueify-campaign-processor}"
email="${EMAIL_SERVICE_DIR:-$(dirname "$campaign")/queueify-email-service}"
frontend="${FRONTEND_DIR:-$HOME/Desktop/Queuify Frontend}"
for jar in "$campaign/target/queueify-campaign-service-0.0.1-SNAPSHOT.jar" "$processor/target/queueify-campaign-processor-0.0.1-SNAPSHOT.jar" "$email/target/queueify-email-service-0.0.1-SNAPSHOT.jar"; do
  [[ -f "$jar" ]] || { echo "Build the missing service jar with ./mvnw -B verify first."; exit 1; }
done
[[ -f "$frontend/node_modules/vite/bin/vite.js" ]] || { echo "Install frontend dependencies first."; exit 1; }
for port in 18080 18081 18082 15173; do
  if lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then echo "Demo port $port is occupied; no process was stopped."; exit 1; fi
done
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export KAFKA_START_TOPIC="queueify-demo-$QUEUEIFY_DEMO_ID-start"
export KAFKA_EMAIL_TOPIC="queueify-demo-$QUEUEIFY_DEMO_ID-email"
export KAFKA_START_GROUP="queueify-demo-$QUEUEIFY_DEMO_ID-start"
export KAFKA_EMAIL_GROUP="queueify-demo-$QUEUEIFY_DEMO_ID-email"
export QUEUEIFY_S3_BASE_PREFIX="queueify-demo/$QUEUEIFY_DEMO_ID"
export PROCESSOR_BASE_URL=http://127.0.0.1:18081
export CAMPAIGN_SERVICE_BASE_URL=http://127.0.0.1:18080
export CAMPAIGN_START_BRIDGE_ENABLED=true PROCESSOR_TASK_API_ENABLED=true PROCESSOR_DELIVERY_API_ENABLED=true
export CAMPAIGN_CONTEXT_API_ENABLED=true CAMPAIGN_SENDER_CREDENTIALS_ENABLED=true CREDENTIAL_ENCRYPTION_ENABLED=true CAMPAIGN_PROGRESS_ENABLED=true
export EMAIL_TASK_LOOKUP_ENABLED=true EMAIL_CAMPAIGN_LOOKUP_ENABLED=true EMAIL_SENDER_LOOKUP_ENABLED=true
export EMAIL_DELIVERY_API_ENABLED=true EMAIL_SMTP_ENABLED=true EMAIL_DELIVERY_WORKFLOW_ENABLED=true EMAIL_CONSUMER_ENABLED=true
export PROCESSOR_RECOVERY_ENABLED=true
compose="$processor/compose.yaml"
[[ -f "$compose" ]] || compose="$processor/docker-compose.yml"
docker compose -f "$compose" up -d --wait kafka
for topic in "$KAFKA_START_TOPIC" "$KAFKA_EMAIL_TOPIC"; do
  docker compose -f "$compose" exec -T kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic "$topic" --partitions 1 --replication-factor 1
done
docker compose -p "queueify-demo-$QUEUEIFY_DEMO_ID" -f "$campaign/scripts/demo-compose.yaml" up -d --wait
logs="${TMPDIR:-/tmp}/queueify-demo-$QUEUEIFY_DEMO_ID"
mkdir -p "$logs"; chmod 700 "$logs"
pids=()
cleanup() { for pid in ${pids[*]:-}; do kill "$pid" 2>/dev/null || true; done; }
trap cleanup EXIT
trap 'exit 130' INT TERM
SPRING_DATASOURCE_URL='jdbc:mysql://localhost:13306/queueify_demo' SPRING_DATASOURCE_USERNAME=root SPRING_DATASOURCE_PASSWORD="$QUEUEIFY_DEMO_DB_PASSWORD" \
  java -jar "$campaign/target/queueify-campaign-service-0.0.1-SNAPSHOT.jar" --server.port=18080 --management.health.mail.enabled=false >"$logs/campaign.log" 2>&1 &
pids+=("$!")
PROCESSOR_DB_URL='jdbc:mysql://localhost:13307/queueify_processor_demo' PROCESSOR_DB_USERNAME=root PROCESSOR_DB_PASSWORD="$QUEUEIFY_DEMO_DB_PASSWORD" \
  java -jar "$processor/target/queueify-campaign-processor-0.0.1-SNAPSHOT.jar" --server.port=18081 >"$logs/processor.log" 2>&1 &
pids+=("$!")
java -jar "$email/target/queueify-email-service-0.0.1-SNAPSHOT.jar" --server.port=18082 >"$logs/email.log" 2>&1 &
pids+=("$!")
(cd "$frontend" && QUEUEIFY_API_TARGET=http://127.0.0.1:18080 node node_modules/vite/bin/vite.js --host 127.0.0.1 --port 15173 --strictPort) >"$logs/frontend.log" 2>&1 &
pids+=("$!")
for port in 18080 18081 18082; do
  ready=false
  for attempt in {1..60}; do
    if curl --silent --fail --connect-timeout 1 --max-time 3 "http://127.0.0.1:$port/actuator/health" >/dev/null; then ready=true; break; fi
    sleep 1
  done
  [[ "$ready" == true ]] || { echo "Service $port is not ready. Inspect private logs in $logs"; exit 1; }
done
echo "Demo ready: http://127.0.0.1:15173 — isolated demo databases/topics. No campaign has been started."
echo "Logs: $logs. Ctrl+C stops only apps started by this script; databases/topics are retained."
wait
