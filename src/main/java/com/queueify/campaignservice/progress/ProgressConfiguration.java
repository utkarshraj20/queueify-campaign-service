package com.queueify.campaignservice.progress;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods=false)
@EnableScheduling
@EnableConfigurationProperties(ProgressProperties.class)
@ConditionalOnProperty(name="queueify.progress.enabled",havingValue="true")
public class ProgressConfiguration {
    @Bean ProcessorProgressClient processorProgressClient(ProgressProperties properties,ObjectMapper mapper) {
        var transport=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();
        var factory=new JdkClientHttpRequestFactory(transport);factory.setReadTimeout(Duration.ofSeconds(5));
        return new ProcessorProgressClient(RestClient.builder().requestFactory(factory)
                .baseUrl(properties.baseUrl().toString().replaceAll("/+$",""))
                .defaultHeader("Authorization","Bearer "+properties.token()).build(),mapper);
    }
}
