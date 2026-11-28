package com.quitmate.appMetrics.ga4.displayname.enums;

import com.quitmate.global.exception.QuitmateException;
import org.springframework.util.StringUtils;

/**
 * 이름을 붙일 GA4 값의 종류(KAN-230). 표 {@code ga4_display_name.value_type}에 이름 그대로 들어간다.
 */
public enum Ga4ValueType {

    /** 화면 이름(GA4 unifiedScreenName). 앱은 expo-router 경로, 웹사이트는 페이지 제목 */
    SCREEN,
    /** 플랫폼(GA4 platform: Android, iOS, web) */
    PLATFORM,
    /** 앱 버전(GA4 appVersion) */
    APP_VERSION;

    public static final String INVALID_TYPE = "값 종류(type)는 SCREEN, PLATFORM, APP_VERSION 중 하나로 보내 주세요.";

    /**
     * @param value 요청 값 그대로(예: "SCREEN")
     * @throws QuitmateException 비었거나 모르는 종류면(400)
     */
    public static Ga4ValueType from(String value) {
        if (!StringUtils.hasText(value)) {
            throw new QuitmateException(INVALID_TYPE);
        }
        String trimmed = value.trim();
        for (Ga4ValueType type : values()) {
            if (type.name().equals(trimmed)) {
                return type;
            }
        }
        throw new QuitmateException(INVALID_TYPE);
    }
}
