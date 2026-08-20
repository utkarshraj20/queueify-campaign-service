package com.queueify.campaignservice.authentication.dto;


import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public class RefreshTokenRequest {

    @NonNull
    private String accessToken ;
}
