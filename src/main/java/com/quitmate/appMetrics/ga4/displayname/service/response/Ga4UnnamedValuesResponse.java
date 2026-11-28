package com.quitmate.appMetrics.ga4.displayname.service.response;

import com.quitmate.appMetrics.ga4.displayname.enums.Ga4SourceType;
import com.quitmate.appMetrics.ga4.displayname.enums.Ga4ValueType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 최근 90일 GA4에 나온 값 중 관리자 이름·기본 이름이 모두 없는 것(KAN-230).
 *
 * @param enabled   GA4 연결 설정이 있으면 true. false면 날짜는 null, values는 빈 배열
 * @param valueType 값 종류
 * @param startDate 시작 날짜(끝 날짜 포함 90일)
 * @param endDate   끝 날짜(GA4 속성 보고 시간대 기준 어제)
 * @param fetchedAt GA4를 실제로 부른 시각. 1시간 캐시에서 줄 때도 그 시각
 * @param values    숫자 내림차순
 */
public record Ga4UnnamedValuesResponse(
        boolean enabled,
        Ga4ValueType valueType,
        LocalDate startDate,
        LocalDate endDate,
        OffsetDateTime fetchedAt,
        List<UnnamedValue> values
) {

    /**
     * @param rawValue    GA4에 들어온 원래 값
     * @param count       화면은 조회 수, 플랫폼·버전은 활성 사용자(90일)
     * @param displayName 지금 보이는 이름(원래 값 또는 '[웹] 원래 값')
     * @param sourceType  짐작한 앱/웹 구분(괄호로 시작하지 않는 화면은 WEB). 모르면 null
     */
    public record UnnamedValue(String rawValue, long count, String displayName, Ga4SourceType sourceType) {
    }
}
