package com.quitmate.appMetrics.ga4.displayname.enums;

import com.quitmate.global.exception.QuitmateException;
import org.springframework.util.StringUtils;

/**
 * 화면이 앱 화면인지 웹사이트 페이지인지(KAN-230). 화면(SCREEN) 이름에만 쓴다.
 * 같은 GA4 속성에 웹사이트가 연결돼 있어 웹 페이지가 앱 화면처럼 보이지 않게 한다.
 */
public enum Ga4SourceType {

    APP,
    WEB;

    public static final String INVALID_SOURCE_TYPE = "앱/웹 구분(sourceType)은 APP, WEB 중 하나로 보내 주세요.";

    /**
     * @param value 요청 값 그대로. 비어 있으면 null(구분 없음)
     * @throws QuitmateException APP·WEB이 아니면(400)
     */
    public static Ga4SourceType fromNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        for (Ga4SourceType type : values()) {
            if (type.name().equals(trimmed)) {
                return type;
            }
        }
        throw new QuitmateException(INVALID_SOURCE_TYPE);
    }
}
