package com.quitmate.appMetrics.ga4.controller;

import com.quitmate.appMetrics.ga4.client.Ga4DataApiClient;
import com.quitmate.appMetrics.ga4.client.Ga4Report;
import com.quitmate.appMetrics.ga4.client.Ga4Report.Row;
import com.quitmate.appMetrics.ga4.client.Ga4ReportRequest;
import com.quitmate.appMetrics.ga4.client.Ga4UnavailableException;
import com.quitmate.global.jwt.JwtTokenGenerator;
import com.quitmate.global.jwt.dto.LoginUserInfo;
import com.quitmate.user.users.entity.User;
import com.quitmate.user.users.entity.enums.Role;
import com.quitmate.user.users.entity.enums.SettingStatus;
import com.quitmate.user.users.entity.enums.Sex;
import com.quitmate.user.users.entity.enums.SnsType;
import com.quitmate.user.users.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GA4 연결 설정이 있을 때를 실제 SecurityConfig·JWT 필터·전역 예외 처리로 확인한다.
 * GA4 호출({@link Ga4DataApiClient})은 가짜로 바꿔 실제 Google에는 연결하지 않는다.
 *
 * <p>서비스의 기간별 캐시가 테스트 사이에 남으므로 테스트마다 다른 period를 쓴다(28: 성공·캐시, 7: 실패 뒤 성공, 90: 계속 실패).
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app-metrics.ga4.property-id=123456789",
        "app-metrics.ga4.credentials-path=/nonexistent/ga4-test-key.json"
})
class Ga4MetricsConnectedApiTest {

    private static final String URL = "/api/v1/app-metrics/ga4";
    private static final String PROPERTY_ID = "123456789";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenGenerator jwtTokenGenerator;
    @Autowired
    private UserRepository userRepository;
    @MockitoBean
    private Ga4DataApiClient ga4DataApiClient;

    private User admin;
    private User member;

    @BeforeEach
    void setUp() {
        admin = userRepository.save(user("admin@test.com", Role.ADMIN));
        member = userRepository.save(user("member@test.com", Role.USER));
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
    }

    @DisplayName("관리자: 200 + 계약 모양 그대로(날짜는 YYYY-MM-DD, fetchedAt은 +09:00). 1시간 안 두 번째 요청은 GA4를 다시 부르지 않는다")
    @Test
    void connected() throws Exception {
        // given
        LocalDate yesterday = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
        given(ga4DataApiClient.runReport(eq(PROPERTY_ID), any()))
                .willAnswer(invocation -> report(invocation.getArgument(1), yesterday));
        String token = token(admin);

        // when·then
        mockMvc.perform(get(URL).param("period", "28").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.period").value(28))
                .andExpect(jsonPath("$.data.endDate").value(yesterday.toString()))
                .andExpect(jsonPath("$.data.startDate").value(yesterday.minusDays(27).toString()))
                .andExpect(jsonPath("$.data.timeZone").value("Asia/Seoul"))
                .andExpect(jsonPath("$.data.fetchedAt").value(endsWith("+09:00")))
                .andExpect(jsonPath("$.data.consoleUrl")
                        .value("https://analytics.google.com/analytics/web/#/p123456789/reports/intelligenthome"))
                .andExpect(jsonPath("$.data.summary.dau").value(120))
                .andExpect(jsonPath("$.data.summary.wau").value(410))
                .andExpect(jsonPath("$.data.summary.mau").value(905))
                .andExpect(jsonPath("$.data.summary.newUsers").value(85))
                .andExpect(jsonPath("$.data.summary.activeUsers").value(1002))
                .andExpect(jsonPath("$.data.dailyActiveUsers", hasSize(28)))
                .andExpect(jsonPath("$.data.dailyActiveUsers[27].date").value(yesterday.toString()))
                .andExpect(jsonPath("$.data.dailyActiveUsers[27].activeUsers").value(118))
                .andExpect(jsonPath("$.data.dailyActiveUsers[0].activeUsers").value(0))
                .andExpect(jsonPath("$.data.topScreens[0].screenName").value("Home"))
                .andExpect(jsonPath("$.data.topScreens[0].views").value(1234))
                .andExpect(jsonPath("$.data.platforms[0].platform").value("iOS"))
                .andExpect(jsonPath("$.data.platforms[1].platform").value("Android"))
                .andExpect(jsonPath("$.data.appVersions[0].appVersion").value("1.1.2"))
                .andExpect(jsonPath("$.data.appVersions[0].activeUsers").value(700));

        mockMvc.perform(get(URL).param("period", "28").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));
        verify(ga4DataApiClient, times(5)).runReport(eq(PROPERTY_ID), any());
    }

    @DisplayName("일반 회원 토큰은 연결돼 있어도 403이고 GA4를 부르지 않는다")
    @Test
    void memberIsForbidden() throws Exception {
        mockMvc.perform(get(URL).param("period", "28").header("Authorization", token(member)))
                .andExpect(status().isForbidden());
        verify(ga4DataApiClient, never()).runReport(any(), any());
    }

    @DisplayName("GA4 호출 실패: 503 + message GA4_UNAVAILABLE + data null (전역 500으로 흘리지 않음)")
    @Test
    void unavailable() throws Exception {
        given(ga4DataApiClient.runReport(eq(PROPERTY_ID), any()))
                .willThrow(new Ga4UnavailableException("GA4 오류 응답 403 PERMISSION_DENIED(summary)."));

        mockMvc.perform(get(URL).param("period", "90").header("Authorization", token(admin)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.statusCode").value(503))
                .andExpect(jsonPath("$.httpStatus").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("GA4_UNAVAILABLE"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @DisplayName("실패는 캐시하지 않는다: 503 다음 요청은 GA4를 다시 불러 200")
    @Test
    void failureIsNotCached() throws Exception {
        LocalDate yesterday = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
        given(ga4DataApiClient.runReport(eq(PROPERTY_ID), any()))
                .willThrow(new Ga4UnavailableException("GA4를 부르지 못했습니다(summary, SocketTimeoutException)."))
                .willAnswer(invocation -> report(invocation.getArgument(1), yesterday));
        String token = token(admin);

        mockMvc.perform(get(URL).param("period", "7").header("Authorization", token))
                .andExpect(status().isServiceUnavailable());
        mockMvc.perform(get(URL).param("period", "7").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dailyActiveUsers", hasSize(7)));
        verify(ga4DataApiClient, times(1 + 5)).runReport(eq(PROPERTY_ID), any());
    }

    private static Ga4Report report(Ga4ReportRequest request, LocalDate yesterday) {
        List<Row> rows = switch (request.name()) {
            case "summary" -> List.of(new Row(List.of(), List.of(120L, 410L, 905L, 85L, 1002L)));
            case "dailyActiveUsers" -> List.of(new Row(List.of(yesterday.toString().replace("-", "")), List.of(118L)));
            case "topScreens" -> List.of(new Row(List.of("Home"), List.of(1234L)));
            case "platforms" -> List.of(new Row(List.of("Android"), List.of(402L)), new Row(List.of("iOS"), List.of(600L)));
            case "appVersions" -> List.of(new Row(List.of("1.1.2"), List.of(700L)));
            default -> throw new IllegalArgumentException(request.name());
        };
        return new Ga4Report("Asia/Seoul", rows);
    }

    private String token(User account) throws Exception {
        return "Bearer " + jwtTokenGenerator.generate(LoginUserInfo.of(account.getId())).getAccessToken();
    }

    private static User user(String email, Role role) {
        return User.builder()
                .email(email)
                .password("encoded")
                .nickName("닉네임")
                .phoneNumber("010-0000-0000")
                .sex(Sex.MALE)
                .role(role)
                .snsType(SnsType.NORMAL)
                .settingStatus(SettingStatus.COMPLETE)
                .build();
    }
}
