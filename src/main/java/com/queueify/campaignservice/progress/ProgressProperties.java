package com.queueify.campaignservice.progress;

import java.net.URI;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="queueify.progress")
public record ProgressProperties(URI baseUrl,String token) {
    public ProgressProperties {
        if(baseUrl==null || baseUrl.getHost()==null || baseUrl.getUserInfo()!=null || baseUrl.getQuery()!=null
                || baseUrl.getFragment()!=null || (baseUrl.getPath()!=null && !Set.of("","/").contains(baseUrl.getPath()))) throw new IllegalArgumentException("Progress URL must be a service origin.");
        boolean local=Set.of("localhost","127.0.0.1","::1","[::1]").contains(baseUrl.getHost());
        if(!"https".equals(baseUrl.getScheme()) && !(local && "http".equals(baseUrl.getScheme()))) throw new IllegalArgumentException("Progress URL requires HTTPS except for loopback.");
        if(token==null || !token.matches("[A-Za-z0-9_-]{32,256}")) throw new IllegalArgumentException("Progress requires a valid internal task token.");
    }
    @Override public String toString() {return "ProgressProperties[token=REDACTED]";}
}
