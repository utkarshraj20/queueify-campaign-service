package com.queueify.campaignservice.progress;

import com.fasterxml.jackson.databind.*;
import java.util.Optional;
import org.springframework.web.client.*;

public class ProcessorProgressClient {
    private final RestClient http;
    private final ObjectReader reader;
    public ProcessorProgressClient(RestClient http,ObjectMapper mapper) {
        this.http=http;
        reader=mapper.copy().disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES,DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES,
                        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readerFor(RunProgress.class);
    }
    public Optional<RunProgress> fetch(CampaignProgressRepository.Candidate candidate) {
        try {
            var response=http.get().uri("/internal/v1/campaigns/{id}/runs/{run}/progress",candidate.campaignId(),candidate.runId())
                    .retrieve().onStatus(status->status.value()!=200,(request,result)->{if(result.getStatusCode().value()!=404) throw new ProgressLookupException();})
                    .toEntity(String.class);
            if(response.getStatusCode().value()==404) return Optional.empty();
            if(response.getBody()==null) throw new ProgressLookupException();
            RunProgress value=reader.readValue(response.getBody());
            if(value==null || value.campaignId()!=candidate.campaignId() || !value.runId().equals(candidate.runId())) throw new ProgressLookupException();
            return Optional.of(value);
        } catch(java.io.IOException | IllegalArgumentException | RestClientException invalid) {throw new ProgressLookupException();}
    }
}
