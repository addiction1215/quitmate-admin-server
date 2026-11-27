package docs.appMetrics;

import com.quitmate.appMetrics.common.AppMetricsPeriod;
import com.quitmate.appMetrics.ga4.controller.Ga4MetricsController;
import com.quitmate.appMetrics.ga4.displayname.enums.Ga4SourceType;
import com.quitmate.appMetrics.ga4.service.Ga4MetricsService;
import com.quitmate.appMetrics.ga4.service.response.Ga4MetricsResponse;
import com.quitmate.appMetrics.ga4.service.response.Ga4MetricsResponse.ActiveUserCounts;
import com.quitmate.appMetrics.ga4.service.response.Ga4MetricsResponse.ActiveUsersByPlatform;
import com.quitmate.appMetrics.ga4.service.response.Ga4MetricsResponse.AppVersion;
import com.quitmate.appMetrics.ga4.service.response.Ga4MetricsResponse.DailyActiveUsers;
import com.quitmate.appMetrics.ga4.service.response.Ga4MetricsResponse.Platform;
import com.quitmate.appMetrics.ga4.service.response.Ga4MetricsResponse.Screen;
import com.quitmate.appMetrics.ga4.service.response.Ga4MetricsResponse.Summary;
import docs.RestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class Ga4MetricsControllerDocsTest extends RestDocsSupport {

    private static final OffsetDateTime FETCHED_AT = OffsetDateTime.parse("2026-11-19T22:10:00+09:00");

    private final Ga4MetricsService ga4MetricsService = mock(Ga4MetricsService.class);

    @Override
    protected Object initController() {
        return new Ga4MetricsController(ga4MetricsService);
    }

    @DisplayName("앱 지표 GA4 조회 API")
    @Test
    void 앱_지표_GA4_조회_API() throws Exception {
        LocalDate end = LocalDate.of(2026, 11, 18);
        given(ga4MetricsService.getMetrics(any())).willReturn(new Ga4MetricsResponse(
                true, 7, end.minusDays(6), end, "Asia/Seoul", FETCHED_AT,
                "https://analytics.google.com/analytics/web/#/p123456789/reports/intelligenthome",
                new Summary(140, 470, 1100, 85, 1002),
                List.of(new DailyActiveUsers(end.minusDays(6), 118), new DailyActiveUsers(end.minusDays(5), 0),
                        new DailyActiveUsers(end.minusDays(4), 121), new DailyActiveUsers(end.minusDays(3), 117),
                        new DailyActiveUsers(end.minusDays(2), 130), new DailyActiveUsers(end.minusDays(1), 126),
                        new DailyActiveUsers(end, 120)),
                List.of(new Screen("(main)/(tabs)", 1234, "[앱] 홈 탭", Ga4SourceType.APP),
                        new Screen("금연 앱 Quitmate", 410, "[웹] 금연 앱 Quitmate", Ga4SourceType.WEB)),
                List.of(new Platform("iOS", 600, "앱(iOS)"), new Platform("web", 402, "웹사이트")),
                List.of(new AppVersion("1.1.2", 700, "1.1.2"), new AppVersion("(not set)", 280, "버전 없음(웹사이트)")),
                new ActiveUsersByPlatform(new ActiveUserCounts(120, 400, 950), new ActiveUserCounts(50, 200, 500),
                        new ActiveUserCounts(80, 230, 520), new ActiveUserCounts(30, 90, 200))));

        mockMvc.perform(get("/api/v1/app-metrics/ga4").param("period", "7"))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("app-metrics-ga4",
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("period").description("조회 기간(일): 7, 28, 90 중 하나. 그 밖·빈 값은 400")
                        ),
                        responseFields(
                                fieldWithPath("statusCode").type(JsonFieldType.NUMBER).description("코드"),
                                fieldWithPath("httpStatus").type(JsonFieldType.STRING).description("상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메세지"),
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("GA4 지표"),
                                fieldWithPath("data.enabled").type(JsonFieldType.BOOLEAN).description("GA4 연결 설정이 있으면 true. false면 아래 값은 null·빈 배열(연결 전)"),
                                fieldWithPath("data.period").type(JsonFieldType.NUMBER).description("조회 기간(일)"),
                                fieldWithPath("data.startDate").type(JsonFieldType.STRING).description("시작 날짜 yyyy-MM-dd(끝 날짜 포함 period일)"),
                                fieldWithPath("data.endDate").type(JsonFieldType.STRING).description("끝 날짜 yyyy-MM-dd. GA4 속성 보고 시간대 기준 어제(마지막으로 다 찬 날)"),
                                fieldWithPath("data.timeZone").type(JsonFieldType.STRING).description("GA4 속성 보고 시간대(GA4 응답 그대로. 응답에 없으면 Asia/Seoul)"),
                                fieldWithPath("data.fetchedAt").type(JsonFieldType.STRING).description("GA4를 실제로 부른 시각(+09:00). 캐시(1시간)에서 줄 때도 그 시각"),
                                fieldWithPath("data.consoleUrl").type(JsonFieldType.STRING).description("GA4 콘솔 바로가기"),
                                fieldWithPath("data.summary").type(JsonFieldType.OBJECT).description("기간 요약"),
                                fieldWithPath("data.summary.dau").type(JsonFieldType.NUMBER).description("끝 날짜 기준 1일 활성 사용자(GA4 active1DayUsers). 모든 플랫폼(웹사이트 포함). 플랫폼별은 activeUsersByPlatform"),
                                fieldWithPath("data.summary.wau").type(JsonFieldType.NUMBER).description("끝 날짜 기준 7일 활성 사용자(GA4 active7DayUsers). 모든 플랫폼"),
                                fieldWithPath("data.summary.mau").type(JsonFieldType.NUMBER).description("끝 날짜 기준 28일 활성 사용자(GA4 active28DayUsers). 모든 플랫폼"),
                                fieldWithPath("data.summary.newUsers").type(JsonFieldType.NUMBER).description("기간 신규 사용자(GA4 newUsers, 앱은 first_open)"),
                                fieldWithPath("data.summary.activeUsers").type(JsonFieldType.NUMBER).description("기간 전체 활성 사용자(GA4 activeUsers). 여러 날 쓴 사람도 1명"),
                                fieldWithPath("data.dailyActiveUsers[]").type(JsonFieldType.ARRAY).description("날짜별 활성 사용자. 날짜 오름차순 period개, 빠진 날은 0"),
                                fieldWithPath("data.dailyActiveUsers[].date").type(JsonFieldType.STRING).description("날짜 yyyy-MM-dd"),
                                fieldWithPath("data.dailyActiveUsers[].activeUsers").type(JsonFieldType.NUMBER).description("그날 활성 사용자(GA4 activeUsers)"),
                                fieldWithPath("data.topScreens[]").type(JsonFieldType.ARRAY).description("화면별 조회 수 상위 10(내림차순)"),
                                fieldWithPath("data.topScreens[].screenName").type(JsonFieldType.STRING).description("화면 원래 값(GA4 unifiedScreenName)"),
                                fieldWithPath("data.topScreens[].views").type(JsonFieldType.NUMBER).description("조회 수(GA4 screenPageViews)"),
                                fieldWithPath("data.topScreens[].displayName").type(JsonFieldType.STRING).description("보일 이름: 관리자 이름 → 기본 이름 → '[웹] 원래 값'(괄호로 시작하지 않는 화면) → 원래 값"),
                                fieldWithPath("data.topScreens[].sourceType").type(JsonFieldType.STRING).optional().description("앱 화면 APP / 웹 페이지 WEB. 모르면 null"),
                                fieldWithPath("data.platforms[]").type(JsonFieldType.ARRAY).description("플랫폼별 활성 사용자(내림차순)"),
                                fieldWithPath("data.platforms[].platform").type(JsonFieldType.STRING).description("플랫폼 원래 값(GA4 platform: iOS, Android, web)"),
                                fieldWithPath("data.platforms[].activeUsers").type(JsonFieldType.NUMBER).description("활성 사용자"),
                                fieldWithPath("data.platforms[].displayName").type(JsonFieldType.STRING).description("보일 이름: 관리자 이름 → 기본 이름 → 원래 값"),
                                fieldWithPath("data.appVersions[]").type(JsonFieldType.ARRAY).description("앱 버전별 활성 사용자 상위 10(내림차순)"),
                                fieldWithPath("data.appVersions[].appVersion").type(JsonFieldType.STRING).description("앱 버전 원래 값(GA4 appVersion)"),
                                fieldWithPath("data.appVersions[].activeUsers").type(JsonFieldType.NUMBER).description("활성 사용자"),
                                fieldWithPath("data.appVersions[].displayName").type(JsonFieldType.STRING).description("보일 이름: 관리자 이름 → 기본 이름 → 원래 값"),
                                fieldWithPath("data.activeUsersByPlatform").type(JsonFieldType.OBJECT).description("DAU·WAU·MAU를 나눈 값(끝 날짜 기준). 두 기기를 쓰는 사람은 android·ios에 1명씩, app에서는 1명"),
                                fieldWithPath("data.activeUsersByPlatform.app").type(JsonFieldType.OBJECT).description("앱 전체: platform이 Android 또는 iOS(GA4에 걸러서 따로 조회)"),
                                fieldWithPath("data.activeUsersByPlatform.android").type(JsonFieldType.OBJECT).description("platform = Android"),
                                fieldWithPath("data.activeUsersByPlatform.ios").type(JsonFieldType.OBJECT).description("platform = iOS"),
                                fieldWithPath("data.activeUsersByPlatform.web").type(JsonFieldType.OBJECT).description("platform = web(웹사이트 방문자, 앱 사용자 수에 넣지 않음)"),
                                fieldWithPath("data.activeUsersByPlatform.*.dau").type(JsonFieldType.NUMBER).description("1일 활성 사용자"),
                                fieldWithPath("data.activeUsersByPlatform.*.wau").type(JsonFieldType.NUMBER).description("7일 활성 사용자"),
                                fieldWithPath("data.activeUsersByPlatform.*.mau").type(JsonFieldType.NUMBER).description("28일 활성 사용자")
                        )
                ));
    }

    @DisplayName("앱 지표 GA4 조회 API - 연결 전")
    @Test
    void 앱_지표_GA4_조회_API_연결_전() throws Exception {
        given(ga4MetricsService.getMetrics(any()))
                .willReturn(Ga4MetricsResponse.disabled(AppMetricsPeriod.DAYS_28, FETCHED_AT));

        mockMvc.perform(get("/api/v1/app-metrics/ga4").param("period", "28"))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("app-metrics-ga4-disabled",
                        preprocessResponse(prettyPrint())
                ));
    }
}
