package com.quitmate.user.users.service.request;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
public class LoginServiceRequest {
    private final String email;
    private final String password;
    private final String deviceId;
    private final String pushKey;

    @Builder
    private LoginServiceRequest(String email, String password, String deviceId, String pushKey) {
        this.email = email;
        this.password = password;
        this.deviceId = deviceId;
        this.pushKey = pushKey;
    }
}
