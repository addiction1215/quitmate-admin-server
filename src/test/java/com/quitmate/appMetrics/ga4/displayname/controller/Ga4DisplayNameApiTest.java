package com.quitmate.appMetrics.ga4.displayname.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quitmate.appMetrics.ga4.client.Ga4DataApiClient;
import com.quitmate.appMetrics.ga4.client.Ga4Report;
import com.quitmate.appMetrics.ga4.client.Ga4Report.Row;
import com.quitmate.appMetrics.ga4.client.Ga4ReportRequest;
import com.quitmate.appMetrics.ga4.displayname.enums.Ga4ValueType;
import com.quitmate.appMetrics.ga4.displayname.repository.Ga4DisplayNameJpaRepository;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 「GA4 이름 설정」 API(KAN-230)를 실제 SecurityConfig·JWT 필터·전역 예외 처리·DB(H2)로 확인한다.
 * GA4 호출은 가짜로 바꿔 실제 Google에는 연결하지 않는다.
 *
 * <p>속성 ID를 다른 GA4 테스트와 다르게 둬서 스프링 컨텍스트(서비스의 GA4 캐시)를 따로 쓴다.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app-metrics.ga4.property-id=987654321",
        "app-metrics.ga4.credentials-path=/nonexistent/ga4-test-key.json"
})
class Ga4DisplayNameApiTest {

    private static final String URL = "/api/v1/app-metrics/ga4/display-names";
    private static final String PROPERTY_ID = "987654321";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtTokenGenerator jwtTokenGenerator;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private Ga4DisplayNameJpaRepository ga4DisplayNameJpaRepository;
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
        ga4DisplayNameJpaRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @DisplayName("목록: 관리자 이름이 없으면 기본 이름표가 설계 문서 순서로 나온다")
    @Test
    void listDefaults() throws Exception {
        mockMvc.perform(get(URL).param("type", "SCREEN").header("Authorization", token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").isEmpty())
                .andExpect(jsonPath("$.data[0].valueType").value("SCREEN"))
                .andExpect(jsonPath("$.data[0].rawValue").value("(main)/(tabs)"))
                .andExpect(jsonPath("$.data[0].displayName").value("[앱] 홈 탭"))
                .andExpect(jsonPath("$.data[0].origin").value("DEFAULT"))
                .andExpect(jsonPath("$.data[0].defaultName").value("[앱] 홈 탭"))
                .andExpect(jsonPath("$.data[0].sourceType").value("APP"))
                .andExpect(jsonPath("$.data[0].updatedBy").isEmpty());

        mockMvc.perform(get(URL).param("type", "PLATFORM").header("Authorization", token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[2].rawValue").value("web"))
                .andExpect(jsonPath("$.data[2].displayName").value("웹사이트"))
                .andExpect(jsonPath("$.data[2].sourceType").isEmpty());
    }

    @DisplayName("저장: 없으면 추가, 있으면 고친다. 이름 앞뒤 공백은 지우고 고친 관리자를 남긴다")
    @Test
    void saveCreatesThenUpdates() throws Exception {
        String token = token(admin);

        long id = idOf(save(token, Map.of("valueType", "SCREEN", "rawValue", "(main)/(tabs)/awards",
                "displayName", "  [앱] 챌린지 목록  ", "sourceType", "APP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("[앱] 챌린지 목록"))
                .andExpect(jsonPath("$.data.origin").value("ADMIN"))
                .andExpect(jsonPath("$.data.defaultName").value("[앱] 챌린지 탭"))
                .andExpect(jsonPath("$.data.updatedBy").value(admin.getId()))
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty()));

        long sameId = idOf(save(token, Map.of("valueType", "SCREEN", "rawValue", "(main)/(tabs)/awards",
                "displayName", "[앱] 챌린지", "sourceType", "APP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("[앱] 챌린지")));

        assertThat(sameId).isEqualTo(id);
        assertThat(ga4DisplayNameJpaRepository.count()).isEqualTo(1);
        mockMvc.perform(get(URL).param("type", "SCREEN").header("Authorization", token))
                .andExpect(jsonPath("$.data[2].rawValue").value("(main)/(tabs)/awards"))
                .andExpect(jsonPath("$.data[2].displayName").value("[앱] 챌린지"))
                .andExpect(jsonPath("$.data[2].origin").value("ADMIN"))
                .andExpect(jsonPath("$.data[2].id").value(id));
    }

    @DisplayName("기본 이름표에 없는 값에도 이름을 넣을 수 있고, 목록에서 기본 이름 뒤에 나온다")
    @Test
    void adminOnlyValue() throws Exception {
        String token = token(admin);
        Map<String, Object> body = new HashMap<>();
        body.put("valueType", "APP_VERSION");
        body.put("rawValue", "1");
        body.put("displayName", "개발 빌드");
        body.put("sourceType", null);
        save(token, body).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.defaultName").isEmpty())
                .andExpect(jsonPath("$.data.sourceType").isEmpty());

        mockMvc.perform(get(URL).param("type", "APP_VERSION").header("Authorization", token))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].rawValue").value("(not set)"))
                .andExpect(jsonPath("$.data[1].rawValue").value("1"))
                .andExpect(jsonPath("$.data[1].displayName").value("개발 빌드"));
    }

    @DisplayName("잘못된 저장은 400: 빈 이름, 100자를 넘는 이름, 모르는 종류, 화면이 아닌데 앱/웹 구분, 모르는 앱/웹 구분, 빈 원래 값")
    @Test
    void invalidSave() throws Exception {
        String token = token(admin);
        List<Map<String, Object>> bodies = List.of(
                Map.of("valueType", "SCREEN", "rawValue", "index", "displayName", "   "),
                Map.of("valueType", "SCREEN", "rawValue", "index", "displayName", "가".repeat(101)),
                Map.of("valueType", "PAGE", "rawValue", "index", "displayName", "시작"),
                Map.of("valueType", "PLATFORM", "rawValue", "web", "displayName", "웹", "sourceType", "WEB"),
                Map.of("valueType", "SCREEN", "rawValue", "index", "displayName", "시작", "sourceType", "MOBILE"),
                Map.of("valueType", "SCREEN", "rawValue", "", "displayName", "시작"),
                Map.of("rawValue", "index", "displayName", "시작"));
        for (Map<String, Object> body : bodies) {
            save(token, body).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400));
        }
        save(token, Map.of("valueType", "SCREEN", "rawValue", "index", "displayName", "가".repeat(101)))
                .andExpect(jsonPath("$.message").value("이름(displayName)은 100자까지 쓸 수 있습니다."));
        save(token, Map.of("valueType", "PLATFORM", "rawValue", "web", "displayName", "웹", "sourceType", "WEB"))
                .andExpect(jsonPath("$.message").value("앱/웹 구분(sourceType)은 화면(SCREEN) 이름에만 넣을 수 있습니다."));
        mockMvc.perform(get(URL).param("type", "screen").header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(Ga4ValueType.INVALID_TYPE));
        mockMvc.perform(get(URL).header("Authorization", token))
                .andExpect(status().isBadRequest());
        // 100자는 된다
        save(token, Map.of("valueType", "SCREEN", "rawValue", "index", "displayName", "가".repeat(100)))
                .andExpect(status().isOk());
        assertThat(ga4DisplayNameJpaRepository.count()).isEqualTo(1);
    }

    @DisplayName("되돌리기: 관리자 이름을 지우고 기본 이름을 돌려준다. 없는 ID는 404")
    @Test
    void deleteRevertsToDefault() throws Exception {
        String token = token(admin);
        long id = idOf(save(token, Map.of("valueType", "PLATFORM", "rawValue", "web", "displayName", "홈페이지")));

        mockMvc.perform(delete(URL + "/{id}", id).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isEmpty())
                .andExpect(jsonPath("$.data.rawValue").value("web"))
                .andExpect(jsonPath("$.data.displayName").value("웹사이트"))
                .andExpect(jsonPath("$.data.origin").value("DEFAULT"));
        assertThat(ga4DisplayNameJpaRepository.count()).isZero();

        mockMvc.perform(delete(URL + "/{id}", id).header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @DisplayName("토큰이 없으면 401, 일반 회원은 403(목록·저장·되돌리기 모두)")
    @Test
    void auth() throws Exception {
        mockMvc.perform(get(URL).param("type", "SCREEN")).andExpect(status().isUnauthorized());
        String memberToken = token(member);
        mockMvc.perform(get(URL).param("type", "SCREEN").header("Authorization", memberToken))
                .andExpect(status().isForbidden());
        save(memberToken, Map.of("valueType", "SCREEN", "rawValue", "index", "displayName", "시작"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(URL + "/{id}", 1).header("Authorization", memberToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(URL + "/unnamed").param("type", "SCREEN").header("Authorization", memberToken))
                .andExpect(status().isForbidden());
        assertThat(ga4DisplayNameJpaRepository.count()).isZero();
        verify(ga4DataApiClient, never()).runReport(any(), any());
    }

    @DisplayName("이름을 저장하면 앱 지표에 바로 보인다: GA4 숫자는 캐시에서 주고 이름만 새로 붙인다")
    @Test
    void savedNameShowsInMetricsImmediately() throws Exception {
        given(ga4DataApiClient.runReport(eq(PROPERTY_ID), any()))
                .willAnswer(invocation -> metricsReport(invocation.getArgument(1)));
        String token = token(admin);

        mockMvc.perform(get("/api/v1/app-metrics/ga4").param("period", "28").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topScreens[0].displayName").value("[앱] 홈 탭"))
                .andExpect(jsonPath("$.data.topScreens[1].displayName").value("[웹] 금연 앱 Quitmate"))
                .andExpect(jsonPath("$.data.topScreens[1].sourceType").value("WEB"));

        save(token, Map.of("valueType", "SCREEN", "rawValue", "금연 앱 Quitmate", "displayName", "[웹] 홈페이지 첫 화면",
                "sourceType", "WEB")).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/app-metrics/ga4").param("period", "28").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topScreens[1].screenName").value("금연 앱 Quitmate"))
                .andExpect(jsonPath("$.data.topScreens[1].displayName").value("[웹] 홈페이지 첫 화면"));
        verify(ga4DataApiClient, times(8)).runReport(eq(PROPERTY_ID), any());
    }

    @DisplayName("이름 없는 값: 최근 90일 값 중 관리자·기본 이름이 모두 없는 것만, 이름을 넣으면 목록에서 빠진다")
    @Test
    void unnamedValues() throws Exception {
        given(ga4DataApiClient.runReport(eq(PROPERTY_ID), argThat(request -> request.name().equals("recentScreens"))))
                .willReturn(new Ga4Report("Asia/Seoul", List.of(
                        new Row(List.of("(main)/(tabs)"), List.of(900L)),
                        new Row(List.of("금연 앱 Quitmate"), List.of(300L)),
                        new Row(List.of("(main)/new-screen"), List.of(20L)))));
        String token = token(admin);

        mockMvc.perform(get(URL + "/unnamed").param("type", "SCREEN").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.valueType").value("SCREEN"))
                .andExpect(jsonPath("$.data.startDate").isNotEmpty())
                .andExpect(jsonPath("$.data.values", hasSize(2)))
                .andExpect(jsonPath("$.data.values[0].rawValue").value("금연 앱 Quitmate"))
                .andExpect(jsonPath("$.data.values[0].count").value(300))
                .andExpect(jsonPath("$.data.values[0].displayName").value("[웹] 금연 앱 Quitmate"))
                .andExpect(jsonPath("$.data.values[0].sourceType").value("WEB"))
                .andExpect(jsonPath("$.data.values[1].rawValue").value("(main)/new-screen"))
                .andExpect(jsonPath("$.data.values[1].sourceType").isEmpty());

        save(token, Map.of("valueType", "SCREEN", "rawValue", "금연 앱 Quitmate", "displayName", "[웹] 홈페이지",
                "sourceType", "WEB")).andExpect(status().isOk());

        mockMvc.perform(get(URL + "/unnamed").param("type", "SCREEN").header("Authorization", token))
                .andExpect(jsonPath("$.data.values", hasSize(1)))
                .andExpect(jsonPath("$.data.values[0].rawValue").value("(main)/new-screen"));
        verify(ga4DataApiClient, times(1)).runReport(eq(PROPERTY_ID), any());
    }

    private ResultActions save(String token, Map<String, Object> body) throws Exception {
        return mockMvc.perform(put(URL).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private long idOf(ResultActions result) throws Exception {
        String json = result.andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).at("/data/id").asLong();
    }

    private static Ga4Report metricsReport(Ga4ReportRequest request) {
        List<Row> rows = switch (request.name()) {
            case "summary", "appActiveUsers" -> List.of(new Row(List.of(), List.of(10L, 20L, 30L)));
            case "periodTotals" -> List.of(new Row(List.of(), List.of(5L, 40L)));
            case "topScreens" -> List.of(new Row(List.of("(main)/(tabs)"), List.of(100L)),
                    new Row(List.of("금연 앱 Quitmate"), List.of(50L)));
            default -> List.of();
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
