package com.quitmate.appMetrics.ga4.controller;

import com.quitmate.appMetrics.common.AppMetricsPeriod;
import com.quitmate.appMetrics.ga4.config.Ga4Properties;
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
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 SecurityConfig·JWT 필터로 GA4 지표 API를 확인한다. 이 테스트 환경에는 GA4 연결 설정이 없어 GA4를 부르지 않는다.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class Ga4MetricsApiTest {

    private static final String URL = "/api/v1/app-metrics/ga4";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenGenerator jwtTokenGenerator;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private Ga4Properties ga4Properties;

    private User admin;
    private User member;

    @BeforeEach
    void setUp() {
        // 서버에 설정 파일이 있는 환경에서 돌면 실제 GA4를 부르게 되므로, 연결 전인지 먼저 확인한다.
        assertThat(ga4Properties.isConfigured()).isFalse();
        admin = userRepository.save(user("admin@test.com", Role.ADMIN));
        member = userRepository.save(user("member@test.com", Role.USER));
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
    }

    @DisplayName("토큰이 없으면 401, 일반 회원 토큰은 403")
    @Test
    void auth() throws Exception {
        mockMvc.perform(get(URL).param("period", "28"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(URL).param("period", "28").header("Authorization", token(member)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("관리자 계정만 사용할 수 있습니다."));
    }

    @DisplayName("관리자 토큰: 연결 설정이 없으면 200 + enabled=false, 나머지는 null·빈 배열")
    @Test
    void disabled() throws Exception {
        mockMvc.perform(get(URL).param("period", "28").header("Authorization", token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.period").value(28))
                .andExpect(jsonPath("$.data.fetchedAt").value(endsWith("+09:00")))
                .andExpect(jsonPath("$.data.startDate").isEmpty())
                .andExpect(jsonPath("$.data.endDate").isEmpty())
                .andExpect(jsonPath("$.data.timeZone").isEmpty())
                .andExpect(jsonPath("$.data.consoleUrl").isEmpty())
                .andExpect(jsonPath("$.data.summary").isEmpty())
                .andExpect(jsonPath("$.data.dailyActiveUsers", hasSize(0)))
                .andExpect(jsonPath("$.data.topScreens", hasSize(0)))
                .andExpect(jsonPath("$.data.platforms", hasSize(0)))
                .andExpect(jsonPath("$.data.appVersions", hasSize(0)));
    }

    @DisplayName("period가 7·28·90이 아니거나 없거나 숫자가 아니면 400(서버 오류 500이 아님)")
    @Test
    void invalidPeriod() throws Exception {
        String token = token(admin);
        for (String period : new String[]{"30", "0", "abc", ""}) {
            mockMvc.perform(get(URL).param("period", period).header("Authorization", token))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(AppMetricsPeriod.INVALID_PERIOD));
        }
        mockMvc.perform(get(URL).header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(AppMetricsPeriod.INVALID_PERIOD));
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
