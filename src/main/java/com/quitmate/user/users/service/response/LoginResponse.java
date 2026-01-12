package com.quitmate.user.users.service.response;

import com.quitmate.global.jwt.dto.JwtToken;
import com.quitmate.user.users.entity.User;
import com.quitmate.user.users.entity.enums.SettingStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
public class LoginResponse {
    private final String email;
    private final String accessToken;

    @Builder
    public LoginResponse(String accessToken, String email) {
        this.accessToken = accessToken;
        this.email = email;
    }

    public static LoginResponse createResponse(User user, JwtToken jwtToken) {
        return LoginResponse.builder()
                .email(user.getEmail())
                .accessToken(jwtToken.getAccessToken())
                .build();
    }
}
