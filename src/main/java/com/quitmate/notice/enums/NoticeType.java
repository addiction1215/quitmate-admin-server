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

    /**
     * 광고성 정보인지. 이벤트 공지는 광고성 앱 푸시로 보고, 동의한 사람에게만 (광고) 표시를 붙여 보낸다(KAN-109).
     * 나머지 공지는 서비스 소식이다.
     */
    public boolean isAdvertising() {
        return this == EVENT;
    }
}
