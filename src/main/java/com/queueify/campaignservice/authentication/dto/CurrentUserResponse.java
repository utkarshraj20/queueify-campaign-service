package com.queueify.campaignservice.authentication.dto;

import lombok.Getter;

@Getter
public class CurrentUserResponse {
    private final Long id;
    private final String name;
    private final String email;

    public CurrentUserResponse(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }
}
