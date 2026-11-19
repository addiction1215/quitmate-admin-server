package com.quitmate.appMetrics.ga4.service.response;

import com.quitmate.appMetrics.common.AppMetricsPeriod;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 「앱 지표」 GA4 섹션 응답(KAN-218, KAN-217 계약 1번).
 *
 * @param enabled          GA4 연결 설정(속성 ID·키 파일 경로)이 있으면 true. false면 아래 값은 null·빈 배열
 * @param period           조회 기간(7·28·90)
 * @param startDate        시작 날짜(끝 날짜 포함 period일)
 * @param endDate          끝 날짜. GA4 속성 보고 시간대 기준 어제(마지막으로 다 찬 날)
 * @param timeZone         GA4 속성 보고 시간대(응답 metadata.timeZone)
 * @param fetchedAt        GA4를 실제로 부른 시각. 캐시에서 줄 때도 그 시각
 * @param consoleUrl       GA4 콘솔 바로가기
 * @param summary          기간 요약
 * @param dailyActiveUsers 날짜별 활성 사용자. 빠진 날은 0, 날짜 오름차순 period개
 * @param topScreens       화면별 조회 수 상위 10
 * @param platforms        플랫폼별 활성 사용자(내림차순)
 * @param appVersions      앱 버전별 활성 사용자 상위 10
 */
public record Ga4MetricsResponse(
        boolean enabled,
        int period,
        LocalDate startDate,
        LocalDate endDate,
        String timeZone,
        OffsetDateTime fetchedAt,
        String consoleUrl,
        Summary summary,
        List<DailyActiveUsers> dailyActiveUsers,
        List<Screen> topScreens,
        List<Platform> platforms,
        List<AppVersion> appVersions
) {

    /** 서버에 GA4 연결 설정이 아직 없을 때 */
    public static Ga4MetricsResponse disabled(AppMetricsPeriod period, OffsetDateTime fetchedAt) {
        return new Ga4MetricsResponse(false, period.days(), null, null, null, fetchedAt, null, null,
                List.of(), List.of(), List.of(), List.of());
    }

    /**
     * @param dau         끝 날짜 기준 1일 활성 사용자(GA4 active1DayUsers)
     * @param wau         끝 날짜 기준 7일 활성 사용자(GA4 active7DayUsers)
     * @param mau         끝 날짜 기준 28일 활성 사용자(GA4 active28DayUsers)
     * @param newUsers    기간 신규 사용자(GA4 newUsers, 앱은 first_open)
     * @param activeUsers 기간 전체 활성 사용자(GA4 activeUsers)
     */
    public record Summary(long dau, long wau, long mau, long newUsers, long activeUsers) {
    }

    public record DailyActiveUsers(LocalDate date, long activeUsers) {
    }

    /** @param views 조회 수(GA4 screenPageViews) */
    public record Screen(String screenName, long views) {
    }

    public record Platform(String platform, long activeUsers) {
    }

    public record AppVersion(String appVersion, long activeUsers) {
    }
}
