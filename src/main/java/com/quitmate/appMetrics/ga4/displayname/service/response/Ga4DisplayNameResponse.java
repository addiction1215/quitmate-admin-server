package com.quitmate.appMetrics.ga4.displayname.service.response;

import com.quitmate.appMetrics.ga4.displayname.entity.Ga4DisplayName;
import com.quitmate.appMetrics.ga4.displayname.enums.Ga4NameOrigin;
import com.quitmate.appMetrics.ga4.displayname.enums.Ga4SourceType;
import com.quitmate.appMetrics.ga4.displayname.enums.Ga4ValueType;
import com.quitmate.appMetrics.ga4.displayname.service.Ga4DisplayNames;

import java.time.LocalDateTime;

/**
 * GA4 원래 값 하나의 지금 이름(KAN-230).
 *
 * @param id          관리자 이름 행 ID. 관리자 이름이 없으면 null(되돌릴 것이 없음)
 * @param valueType   값 종류
 * @param rawValue    GA4에 들어온 원래 값
 * @param displayName 지금 보이는 이름
 * @param origin      이름이 어디서 왔는지(ADMIN·DEFAULT·NONE)
 * @param defaultName 기본 이름표의 이름. 없으면 null
 * @param sourceType  앱/웹 구분(화면만). 모르면 null
 * @param updatedBy   관리자 이름을 마지막으로 고친 관리자 userId. 관리자 이름이 없으면 null
 * @param updatedAt   관리자 이름을 마지막으로 고친 시각. 관리자 이름이 없으면 null
 */
public record Ga4DisplayNameResponse(
        Long id,
        Ga4ValueType valueType,
        String rawValue,
        String displayName,
        Ga4NameOrigin origin,
        String defaultName,
        Ga4SourceType sourceType,
        Long updatedBy,
        LocalDateTime updatedAt
) {

    public static Ga4DisplayNameResponse of(Ga4ValueType type, String rawValue, Ga4DisplayNames.Resolved resolved) {
        Ga4DisplayName admin = resolved.admin();
        return new Ga4DisplayNameResponse(
                admin == null ? null : admin.getId(),
                type,
                rawValue,
                resolved.displayName(),
                resolved.origin(),
                resolved.defaultName(),
                resolved.sourceType(),
                admin == null ? null : admin.getUpdatedBy(),
                admin == null ? null : admin.getUpdatedDate());
    }
}
