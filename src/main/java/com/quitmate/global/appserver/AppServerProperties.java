package com.quitmate.global.appserver;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 관리자 서버가 앱 서버 API를 부를 때 쓰는 설정. 값은 배포 환경 변수로만 받는다.
 *
 * @param baseUrl        앱 서버 주소. 환경 변수 APP_SERVER_BASE_URL (예: https://api.example.com, 끝의 / 없이)
 * @param jwtSecretKey   앱 서버 JWT 서명 키(앱 서버 jwt.secret-key와 같은 값). 환경 변수 APP_SERVER_JWT_SECRET_KEY
 * @param tokenTtl       앱 서버로 보내는 관리자 토큰 유효 시간
 * @param connectTimeout 연결 제한 시간
 * @param readTimeout    응답 제한 시간. 재처리는 앱 서버가 요청 안에서 끝까지 실행하므로 넉넉히 둔다.
 */
@ConfigurationProperties(prefix = "app-server")
public record AppServerProperties(
        String baseUrl,
        String jwtSecretKey,
        @DefaultValue("5m") Duration tokenTtl,
        @DefaultValue("3s") Duration connectTimeout,
        @DefaultValue("50s") Duration readTimeout
) {

    /**
     * 빌드 때 환경 변수가 없으면 Gradle expand()가 "null" 글자를 넣는다. 빈 값과 같이 설정이 없는 것으로 본다.
     */
    static boolean isMissing(String value) {
        return !StringUtils.hasText(value) || "null".equals(value.trim());
    }

    boolean hasBaseUrl() {
        return !isMissing(baseUrl);
    }

    boolean hasJwtSecretKey() {
        return !isMissing(jwtSecretKey);
    }

    String normalizedBaseUrl() {
        String url = baseUrl.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}
