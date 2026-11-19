package com.quitmate.appMetrics.ga4.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * GA4 연결 설정(KAN-218). 값은 운영 서버 파일({@link Ga4Config})에서만 받는다. 저장소·jar에는 값을 두지 않는다.
 *
 * @param propertyId      GA4 속성 ID(숫자). {@code properties/123456789} 형식도 받는다. 키 {@code app-metrics.ga4.property-id}
 * @param credentialsPath 서비스 계정 키 json 파일 경로. 키 {@code app-metrics.ga4.credentials-path}
 */
@ConfigurationProperties(prefix = "app-metrics.ga4")
public record Ga4Properties(String propertyId, String credentialsPath) {

    private static final Pattern DIGITS = Pattern.compile("\\d+");
    private static final String PROPERTY_PREFIX = "properties/";

    /** 숫자만 남긴 속성 ID. 없거나 숫자가 아니면 빈 값 */
    public Optional<String> normalizedPropertyId() {
        if (isMissing(propertyId)) {
            return Optional.empty();
        }
        String id = propertyId.trim();
        if (id.startsWith(PROPERTY_PREFIX)) {
            id = id.substring(PROPERTY_PREFIX.length());
        }
        return DIGITS.matcher(id).matches() ? Optional.of(id) : Optional.empty();
    }

    /** 서비스 계정 키 파일 경로. 없거나 경로로 읽을 수 없으면 빈 값(파일이 실제로 있는지는 토큰을 받을 때 본다) */
    public Optional<Path> credentialsFile() {
        if (isMissing(credentialsPath)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Path.of(credentialsPath.trim()));
        } catch (InvalidPathException e) {
            return Optional.empty();
        }
    }

    /** 속성 ID와 키 파일 경로가 둘 다 있어야 GA4를 부른다. 하나라도 없으면 '연결 전' */
    public boolean isConfigured() {
        return normalizedPropertyId().isPresent() && credentialsFile().isPresent();
    }

    /** 값을 하나라도 넣었는지(넣었는데 연결 전이면 형식이 틀린 것) */
    public boolean hasAnyValue() {
        return !isMissing(propertyId) || !isMissing(credentialsPath);
    }

    private static boolean isMissing(String value) {
        return !StringUtils.hasText(value) || "null".equals(value.trim());
    }
}
