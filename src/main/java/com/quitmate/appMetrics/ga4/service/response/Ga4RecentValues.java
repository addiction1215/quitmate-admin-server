package com.quitmate.appMetrics.ga4.service.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 최근 90일 GA4에 들어온 한 종류(화면·플랫폼·버전)의 원래 값과 숫자(KAN-230). 이름 없는 값을 찾을 때 쓴다.
 *
 * @param enabled   GA4 연결 설정이 있으면 true. false면 날짜는 null, values는 빈 배열
 * @param startDate 시작 날짜(끝 날짜 포함 90일)
 * @param endDate   끝 날짜(GA4 속성 보고 시간대 기준 어제)
 * @param fetchedAt GA4를 실제로 부른 시각
 * @param values    숫자 내림차순. 화면은 조회 수, 플랫폼·버전은 활성 사용자
 */
public record Ga4RecentValues(boolean enabled, LocalDate startDate, LocalDate endDate, OffsetDateTime fetchedAt,
                              List<ValueCount> values) {

    public static Ga4RecentValues disabled(OffsetDateTime fetchedAt) {
        return new Ga4RecentValues(false, null, null, fetchedAt, List.of());
    }

    public record ValueCount(String rawValue, long count) {
    }
}
