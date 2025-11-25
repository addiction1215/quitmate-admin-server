package com.quitmate.user.users.service.response;

import com.quitmate.user.users.entity.User;
import com.quitmate.user.users.entity.enums.SettingStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
public class LoginResponse {
    private final String email;
    private final SettingStatus settingStatus;

    @Builder
    private LoginResponse(String email, SettingStatus settingStatus) {
        this.email = email;
        this.settingStatus = settingStatus;
    }

    public static LoginResponse createResponse(User user) {
        return LoginResponse.builder()
                .email(user.getEmail())
                .settingStatus(user.getSettingStatus())
                .build();
    }
}
