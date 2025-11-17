package com.quitmate.global.security;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginUserInfo {
    private Long userId;

    @Builder
    private LoginUserInfo(Long userId) {
        this.userId = userId;
    }

    public static LoginUserInfo of(Long userId) {
        return LoginUserInfo.builder()
                .userId(userId)
                .build();
    }
}