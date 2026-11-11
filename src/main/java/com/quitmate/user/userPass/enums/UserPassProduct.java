package com.quitmate.user.userPass.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 이용권 상품. 지금은 유료 서비스 QuitMate 90 하나다. */
@Getter
@RequiredArgsConstructor
public enum UserPassProduct {
    QUITMATE_90("QuitMate 90");

    private final String description;
}
