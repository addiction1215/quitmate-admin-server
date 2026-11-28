package com.quitmate.appMetrics.ga4.service.response;

import com.quitmate.appMetrics.common.AppMetricsPeriod;
import com.quitmate.appMetrics.ga4.displayname.enums.Ga4SourceType;
import com.quitmate.appMetrics.ga4.displayname.enums.Ga4ValueType;
import com.quitmate.appMetrics.ga4.displayname.service.Ga4DisplayNames;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 「앱 지표」 GA4 섹션 응답(KAN-218, KAN-217 계약 1번. KAN-230에서 이름·플랫폼별 활성 사용자를 더함).
 *
 * @param enabled               GA4 연결 설정(속성 ID·키 파일 경로)이 있으면 true. false면 아래 값은 null·빈 배열
 * @param period                조회 기간(7·28·90)
 * @param startDate             시작 날짜(끝 날짜 포함 period일)
 * @param endDate               끝 날짜. GA4 속성 보고 시간대 기준 어제(마지막으로 다 찬 날)
 * @param timeZone              GA4 속성 보고 시간대(응답 metadata.timeZone)
 * @param fetchedAt             GA4를 실제로 부른 시각. 캐시에서 줄 때도 그 시각
 * @param consoleUrl            GA4 콘솔 바로가기
 * @param summary               기간 요약. DAU·WAU·MAU는 모든 플랫폼(웹사이트 포함)
 * @param dailyActiveUsers      날짜별 활성 사용자. 빠진 날은 0, 날짜 오름차순 period개
 * @param topScreens            화면별 조회 수 상위 10
 * @param platforms             플랫폼별 활성 사용자(내림차순)
 * @param appVersions           앱 버전별 활성 사용자 상위 10
 * @param activeUsersByPlatform DAU·WAU·MAU를 앱 전체·Android·iOS·웹으로 나눈 값(KAN-230)
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
        List<AppVersion> appVersions,
        ActiveUsersByPlatform activeUsersByPlatform
) {

    /** 서버에 GA4 연결 설정이 아직 없을 때 */
    public static Ga4MetricsResponse disabled(AppMetricsPeriod period, OffsetDateTime fetchedAt) {
        return new Ga4MetricsResponse(false, period.days(), null, null, null, fetchedAt, null, null,
                List.of(), List.of(), List.of(), List.of(), null);
    }

    /**
     * 화면·플랫폼·버전에 지금 이름표의 이름을 붙인 사본. GA4 숫자는 캐시해도 이름은 요청마다 붙여서,
     * 관리자가 이름을 바꾸면 바로 보인다.
     */
    public Ga4MetricsResponse withDisplayNames(Ga4DisplayNames names) {
        return new Ga4MetricsResponse(enabled, period, startDate, endDate, timeZone, fetchedAt, consoleUrl, summary,
                dailyActiveUsers,
                topScreens.stream().map(screen -> screen.named(names)).toList(),
                platforms.stream().map(platform -> platform.named(names)).toList(),
                appVersions.stream().map(appVersion -> appVersion.named(names)).toList(),
                activeUsersByPlatform);
    }

    /**
     * @param dau         끝 날짜 기준 1일 활성 사용자(GA4 active1DayUsers)
     * @param wau         끝 날짜 기준 7일 활성 사용자(GA4 active7DayUsers)
     * @param mau         끝 날짜 기준 28일 활성 사용자(GA4 active28DayUsers)
     * @param newUsers    기간 신규 사용자(GA4 newUsers, 앱은 first_open)
     * @param activeUsers 기간 전체 활성 사용자(GA4 activeUsers). 기간 안에 여러 날 쓴 사람도 1명
     */
    public record Summary(long dau, long wau, long mau, long newUsers, long activeUsers) {
    }

    public record DailyActiveUsers(LocalDate date, long activeUsers) {
    }

    /**
     * @param screenName  GA4에 들어온 원래 값(unifiedScreenName)
     * @param views       조회 수(GA4 screenPageViews)
     * @param displayName 보일 이름(관리자 이름 → 기본 이름 → '[웹] 원래 값' → 원래 값)
     * @param sourceType  앱 화면·웹 페이지 구분. 모르면 null
     */
    public record Screen(String screenName, long views, String displayName, Ga4SourceType sourceType) {

        public Screen(String screenName, long views) {
            this(screenName, views, null, null);
        }

        Screen named(Ga4DisplayNames names) {
            Ga4DisplayNames.Resolved resolved = names.resolve(Ga4ValueType.SCREEN, screenName);
            return new Screen(screenName, views, resolved.displayName(), resolved.sourceType());
        }
    }

    /** @param displayName 보일 이름(관리자 이름 → 기본 이름 → 원래 값) */
    public record Platform(String platform, long activeUsers, String displayName) {

        public Platform(String platform, long activeUsers) {
            this(platform, activeUsers, null);
        }

        Platform named(Ga4DisplayNames names) {
            return new Platform(platform, activeUsers, names.resolve(Ga4ValueType.PLATFORM, platform).displayName());
        }
    }

    /** @param displayName 보일 이름(관리자 이름 → 기본 이름 → 원래 값) */
    public record AppVersion(String appVersion, long activeUsers, String displayName) {

        public AppVersion(String appVersion, long activeUsers) {
            this(appVersion, activeUsers, null);
        }

        AppVersion named(Ga4DisplayNames names) {
            return new AppVersion(appVersion, activeUsers,
                    names.resolve(Ga4ValueType.APP_VERSION, appVersion).displayName());
        }
    }

    /** 끝 날짜 기준 1·7·28일 활성 사용자 */
    public record ActiveUserCounts(long dau, long wau, long mau) {

        public static final ActiveUserCounts ZERO = new ActiveUserCounts(0, 0, 0);
    }

    /**
     * DAU·WAU·MAU를 나눈 값(KAN-230). 두 기기를 다 쓰는 사람은 android·ios에 1명씩 세고 app에서는 1명이다.
     *
     * @param app     앱 전체: platform이 Android 또는 iOS인 사람(GA4에 걸러서 따로 조회)
     * @param android platform = Android
     * @param ios     platform = iOS
     * @param web     platform = web(웹사이트 방문자, 앱 사용자 수에 넣지 않음)
     */
    public record ActiveUsersByPlatform(ActiveUserCounts app, ActiveUserCounts android, ActiveUserCounts ios,
                                       ActiveUserCounts web) {
    }
}
