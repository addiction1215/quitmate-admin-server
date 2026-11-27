package com.quitmate.appMetrics.ga4.displayname.service;

import com.quitmate.appMetrics.ga4.displayname.entity.Ga4DisplayName;
import com.quitmate.appMetrics.ga4.displayname.enums.Ga4NameOrigin;
import com.quitmate.appMetrics.ga4.displayname.enums.Ga4SourceType;
import com.quitmate.appMetrics.ga4.displayname.enums.Ga4ValueType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 어느 한 시점의 이름표(관리자 이름 + 기본 이름). GA4 응답에 이름을 붙일 때 요청마다 새로 만든다(KAN-230).
 *
 * <p>이름 고르는 순서(KAN-228 설계 문서 3장):
 * <ol>
 *   <li>종류와 원래 값이 정확히 같은 관리자 이름</li>
 *   <li>없으면 기본 이름표({@link Ga4DefaultDisplayNames})에서 정확히 같은 값</li>
 *   <li>화면인데 둘 다 없고 {@code (}로 시작하지 않으면 웹 페이지로 보고 {@code [웹] 원래 값}.
 *       앱 화면 이름은 expo-router 경로라 {@code (main)/…}처럼 괄호로 시작하거나 {@code index}다.</li>
 *   <li>그래도 없으면 원래 값 그대로</li>
 * </ol>
 */
public final class Ga4DisplayNames {

    static final String WEB_PREFIX = "[웹] ";
    private static final String APP_INDEX_SCREEN = "index";

    private final Map<Ga4ValueType, Map<String, Ga4DisplayName>> adminNames;

    private Ga4DisplayNames(Map<Ga4ValueType, Map<String, Ga4DisplayName>> adminNames) {
        this.adminNames = adminNames;
    }

    public static Ga4DisplayNames of(List<Ga4DisplayName> rows) {
        Map<Ga4ValueType, Map<String, Ga4DisplayName>> byType = new EnumMap<>(Ga4ValueType.class);
        for (Ga4ValueType type : Ga4ValueType.values()) {
            byType.put(type, new HashMap<>());
        }
        for (Ga4DisplayName row : rows) {
            byType.get(row.getValueType()).put(row.getRawValue(), row);
        }
        return new Ga4DisplayNames(byType);
    }

    /** 관리자 이름 없이 기본 이름표만 쓴다 */
    public static Ga4DisplayNames defaultsOnly() {
        return of(List.of());
    }

    public Resolved resolve(Ga4ValueType type, String rawValue) {
        Ga4DisplayName admin = rawValue == null ? null : adminNames.get(type).get(rawValue);
        String defaultName = Ga4DefaultDisplayNames.find(type, rawValue);
        if (admin != null) {
            return new Resolved(admin.getDisplayName(), admin.getSourceType(), Ga4NameOrigin.ADMIN, defaultName, admin);
        }
        if (defaultName != null) {
            return new Resolved(defaultName, defaultSourceType(type), Ga4NameOrigin.DEFAULT, defaultName, null);
        }
        if (type == Ga4ValueType.SCREEN && looksLikeWebPage(rawValue)) {
            return new Resolved(WEB_PREFIX + rawValue, Ga4SourceType.WEB, Ga4NameOrigin.NONE, null, null);
        }
        return new Resolved(rawValue, null, Ga4NameOrigin.NONE, null, null);
    }

    /** 기본 이름표의 화면은 모두 앱 화면이다. 플랫폼·버전은 앱/웹 구분을 쓰지 않는다 */
    private static Ga4SourceType defaultSourceType(Ga4ValueType type) {
        return type == Ga4ValueType.SCREEN ? Ga4SourceType.APP : null;
    }

    private static boolean looksLikeWebPage(String rawValue) {
        return rawValue != null && !rawValue.isBlank()
                && !rawValue.startsWith("(") && !APP_INDEX_SCREEN.equals(rawValue);
    }

    /**
     * @param displayName 보일 이름
     * @param sourceType  앱/웹 구분(화면만). 모르면 null
     * @param origin      이름이 어디서 왔는지
     * @param defaultName 기본 이름표의 이름(관리자 이름을 지우면 돌아갈 이름). 없으면 null
     * @param admin       관리자 이름 행. 없으면 null
     */
    public record Resolved(String displayName, Ga4SourceType sourceType, Ga4NameOrigin origin,
                           String defaultName, Ga4DisplayName admin) {
    }
}
