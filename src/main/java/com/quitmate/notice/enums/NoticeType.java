package com.quitmate.notice.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NoticeType {
    NEW_FEATURE("새 기능 추가"),
    FEATURE_UPDATE("기존 기능 수정"),
    EVENT("이벤트"),
    SERVICE_CHECK("서비스 점검");

    private final String description;
}
