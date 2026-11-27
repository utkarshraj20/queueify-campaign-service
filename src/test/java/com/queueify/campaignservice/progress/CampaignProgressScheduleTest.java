package com.queueify.campaignservice.progress;

import java.net.URI;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CampaignProgressScheduleTest {
    @Test void scansPastLongRunningFirstPageAndIsolatesFailures() {
        var repository=mock(CampaignProgressRepository.class);var client=mock(ProcessorProgressClient.class);
        var page=new ArrayList<CampaignProgressRepository.Candidate>();for(int i=1;i<=100;i++)page.add(new CampaignProgressRepository.Candidate(i,i));
        when(repository.candidates(0,100)).thenReturn(page);when(repository.candidates(100,100)).thenReturn(List.of());
        when(client.fetch(any())).thenReturn(Optional.empty());when(client.fetch(page.getFirst())).thenThrow(new ProgressLookupException());
        var schedule=new CampaignProgressSchedule(repository,client);schedule.refresh();schedule.refresh();schedule.refresh();
        verify(repository,times(2)).candidates(0,100);verify(repository).candidates(100,100);verify(client,times(2)).fetch(page.getLast());
    }
    @Test void defaultsOffAndValidatesOriginAndTokenWithoutLeakingIt() {
        new ApplicationContextRunner().withUserConfiguration(ProgressConfiguration.class)
                .run(context->assertThat(context).doesNotHaveBean(ProcessorProgressClient.class));
        assertThatThrownBy(()->new ProgressProperties(URI.create("http://remote.example"),"private-token")).isInstanceOf(IllegalArgumentException.class).hasMessageNotContaining("private-token");
    }
}
