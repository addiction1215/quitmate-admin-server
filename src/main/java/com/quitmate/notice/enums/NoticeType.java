package com.quitmate.notice.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NoticeType {
    NEW_FEATURE("업데이트"),     // 개편 전 값. 관리자에서는 더 고르지 않고 FEATURE_UPDATE 를 쓴다.
    FEATURE_UPDATE("업데이트"),
    EVENT("이벤트"),
    SERVICE_CHECK("점검"),
    NOTICE("안내");

    private final String description;
}
