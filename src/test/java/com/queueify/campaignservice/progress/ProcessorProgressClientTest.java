package com.queueify.campaignservice.progress;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ProcessorProgressClientTest {
    private static final String JSON="{\"campaignId\":42,\"runId\":\"start-7\",\"runStatus\":\"READY\",\"validRows\":3,\"invalidRows\":0,\"totalTasks\":3,\"sent\":2,\"failed\":0,\"unknown\":0,\"processing\":0,\"pending\":1}";
    private final RestClient.Builder builder=RestClient.builder().baseUrl("http://localhost:8081").defaultHeader("Authorization","Bearer fixture-token");
    private final MockRestServiceServer server=MockRestServiceServer.bindTo(builder).build();
    private final ProcessorProgressClient client=new ProcessorProgressClient(builder.build(),new ObjectMapper());
    private final CampaignProgressRepository.Candidate candidate=new CampaignProgressRepository.Candidate(42,7);
    @Test void authenticatesAndSkipsNotYetCreatedRuns() {
        server.expect(requestTo("http://localhost:8081/internal/v1/campaigns/42/runs/start-7/progress"))
                .andExpect(header("Authorization","Bearer fixture-token")).andRespond(withSuccess(JSON,MediaType.APPLICATION_JSON));
        server.expect(anything()).andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertThat(client.fetch(candidate).orElseThrow().sent()).isEqualTo(2);assertThat(client.fetch(candidate)).isEmpty();server.verify();
    }
    @Test void malformedIdentityCountsAndDependencyFailuresNeverBecomeProgress() {
        for(String body:new String[]{JSON.replace("start-7","start-8"),JSON.replace("\"totalTasks\":3","\"totalTasks\":4"),JSON.replace("\"sent\":2","\"sent\":-1"),JSON+" {}"}) {
            server.expect(anything()).andRespond(withSuccess(body,MediaType.APPLICATION_JSON));
        }
        server.expect(anything()).andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("private-token"));
        for(int i=0;i<5;i++) assertThatThrownBy(()->client.fetch(candidate)).isInstanceOf(ProgressLookupException.class).hasNoCause().hasMessageNotContaining("private-token");
        server.verify();
    }
    @Test void rejectsZeroReadyRunAndOverflowInsteadOfCompleting() {
        assertThatThrownBy(()->new RunProgress(42,"start-7","READY",0L,0L,0,0,0,0,0,0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->new RunProgress(42,"start-7","READY",2147483648L,0L,2147483648L,0,0,0,0,2147483648L)).isInstanceOf(IllegalArgumentException.class);
    }
}
