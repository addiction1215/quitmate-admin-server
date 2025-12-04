package com.quitmate.user.users.service.response;

import com.quitmate.user.users.entity.User;
import com.quitmate.user.users.entity.enums.SettingStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
public class LoginResponse {
    private final String email;

    @Builder
    private LoginResponse(String email) {
        this.email = email;
    }

    public static LoginResponse createResponse(User user) {
        return LoginResponse.builder()
                .email(user.getEmail())
                .build();
    }
}
