package com.quitmate.user.users.service.request;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
public class LoginServiceRequest {
    private final String email;
    private final String password;

    @Builder
    private LoginServiceRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
