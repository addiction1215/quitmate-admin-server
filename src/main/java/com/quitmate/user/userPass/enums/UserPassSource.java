package com.quitmate.user.userPass.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 이용권 출처. 관리자 서버는 ADMIN_GRANT만 만든다. IAP는 인앱 결제를 붙일 때 앱 서버가 만든다. */
@Getter
@RequiredArgsConstructor
public enum UserPassSource {
    ADMIN_GRANT("어드민 부여"),
    IAP("인앱 결제");

    private final String description;
}
