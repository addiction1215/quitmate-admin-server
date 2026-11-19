package com.quitmate.appMetrics.push.controller;

import com.quitmate.appMetrics.common.AppMetricsPeriod;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static com.quitmate.appMetrics.push.PushMetricsFixture.createTablesIfMissing;
import static com.quitmate.appMetrics.push.PushMetricsFixture.dailySmokingOutbox;
import static com.quitmate.appMetrics.push.PushMetricsFixture.delivery;
import static com.quitmate.appMetrics.push.PushMetricsFixture.deleteAll;
import static com.quitmate.appMetrics.push.PushMetricsFixture.pushToken;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 SecurityConfig·JWT 필터와 테스트 DB로 푸시 지표 API를 확인한다.
 *
 * <p>서비스의 기간별 캐시(1시간)가 테스트 사이에 남으므로 테스트마다 다른 period를 쓴다(7: 추적 전, 28: 추적 후).
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class PushMetricsApiTest {

    private static final String URL = "/api/v1/app-metrics/push";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenGenerator jwtTokenGenerator;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User admin;
    private User member;

    @BeforeEach
    void setUp() {
        createTablesIfMissing(jdbcTemplate);
        admin = userRepository.save(user("admin@test.com", Role.ADMIN));
        member = userRepository.save(user("member@test.com", Role.USER));
    }

    @AfterEach
    void tearDown() {
        deleteAll(jdbcTemplate);
        userRepository.deleteAllInBatch();
    }

    @DisplayName("토큰이 없으면 401, 일반 회원 토큰은 403")
    @Test
    void auth() throws Exception {
        mockMvc.perform(get(URL).param("period", "7"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(URL).param("period", "7").header("Authorization", token(member)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("관리자 계정만 사용할 수 있습니다."));
    }

    @DisplayName("관리자: 전달·열림 기록 전이면 ticket·receipt·opened·openRate는 null, 발송 요청·토큰 회원은 숫자")
    @Test
    void notTracked() throws Exception {
        // given: 하루 흡연 시간대 푸시 3건(오늘 2, 6일 전 1), 기간 밖(7일 전) 1건, 토큰 회원 1명
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        dailySmokingOutbox(jdbcTemplate, member.getId(), today.atTime(0, 0), "SENT", null);
        dailySmokingOutbox(jdbcTemplate, member.getId(), today.atTime(8, 0), "SENT", null);
        dailySmokingOutbox(jdbcTemplate, member.getId(), today.minusDays(6).atTime(9, 0), "SENT", null);
        dailySmokingOutbox(jdbcTemplate, member.getId(), today.minusDays(7).atTime(23, 59, 59), "SENT", null);
        pushToken(jdbcTemplate, member.getId(), "device-1", "ExponentPushToken[a]");

        // when·then
        mockMvc.perform(get(URL).param("period", "7").header("Authorization", token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.period").value(7))
                .andExpect(jsonPath("$.data.startDate").value(today.minusDays(6).toString()))
                .andExpect(jsonPath("$.data.endDate").value(today.toString()))
                .andExpect(jsonPath("$.data.fetchedAt").value(endsWith("+09:00")))
                .andExpect(jsonPath("$.data.deliveryTracking").value(false))
                .andExpect(jsonPath("$.data.openTracking").value(false))
                .andExpect(jsonPath("$.data.summary.requested").value(3))
                .andExpect(jsonPath("$.data.summary.ticketOk").isEmpty())
                .andExpect(jsonPath("$.data.summary.ticketError").isEmpty())
                .andExpect(jsonPath("$.data.summary.receiptOk").isEmpty())
                .andExpect(jsonPath("$.data.summary.receiptError").isEmpty())
                .andExpect(jsonPath("$.data.summary.receiptPending").isEmpty())
                .andExpect(jsonPath("$.data.summary.opened").isEmpty())
                .andExpect(jsonPath("$.data.summary.openRate").isEmpty())
                .andExpect(jsonPath("$.data.summary.tokenUsers").value(1))
                .andExpect(jsonPath("$.data.daily", hasSize(7)))
                .andExpect(jsonPath("$.data.daily[0].date").value(today.minusDays(6).toString()))
                .andExpect(jsonPath("$.data.daily[0].requested").value(1))
                .andExpect(jsonPath("$.data.daily[0].ticketError").isEmpty())
                .andExpect(jsonPath("$.data.daily[0].opened").isEmpty())
                .andExpect(jsonPath("$.data.daily[6].requested").value(2))
                .andExpect(jsonPath("$.data.errorCodes", hasSize(0)));
    }

    @DisplayName("관리자: 전달·열림 기록이 있으면 숫자로 채우고 오류 코드를 많은 순으로 준다")
    @Test
    void tracked() throws Exception {
        LocalDateTime now = LocalDate.now(ZoneId.of("Asia/Seoul")).atTime(0, 30);
        long opened = dailySmokingOutbox(jdbcTemplate, member.getId(), now, "SENT", now.plusMinutes(1));
        dailySmokingOutbox(jdbcTemplate, member.getId(), now, "SENT", null);
        delivery(jdbcTemplate, member.getId(), opened, now, "OK", null, "OK", null);
        delivery(jdbcTemplate, member.getId(), null, now, "ERROR", "DeviceNotRegistered", "SKIPPED", null);
        delivery(jdbcTemplate, member.getId(), null, now, "OK", null, "ERROR", "DeviceNotRegistered");
        delivery(jdbcTemplate, member.getId(), null, now, "OK", null, "PENDING", null);

        mockMvc.perform(get(URL).param("period", "28").header("Authorization", token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deliveryTracking").value(true))
                .andExpect(jsonPath("$.data.openTracking").value(true))
                .andExpect(jsonPath("$.data.summary.requested").value(2 + 3))
                .andExpect(jsonPath("$.data.summary.ticketOk").value(3))
                .andExpect(jsonPath("$.data.summary.ticketError").value(1))
                .andExpect(jsonPath("$.data.summary.receiptOk").value(1))
                .andExpect(jsonPath("$.data.summary.receiptError").value(1))
                .andExpect(jsonPath("$.data.summary.receiptPending").value(1))
                .andExpect(jsonPath("$.data.summary.opened").value(1))
                .andExpect(jsonPath("$.data.summary.openRate").value(0.5))
                .andExpect(jsonPath("$.data.daily", hasSize(28)))
                .andExpect(jsonPath("$.data.daily[0].ticketError").value(0))
                .andExpect(jsonPath("$.data.daily[27].ticketError").value(1))
                .andExpect(jsonPath("$.data.daily[27].receiptError").value(1))
                .andExpect(jsonPath("$.data.daily[27].opened").value(1))
                .andExpect(jsonPath("$.data.errorCodes[0].code").value("DeviceNotRegistered"))
                .andExpect(jsonPath("$.data.errorCodes[0].count").value(2));
    }

    @DisplayName("period가 7·28·90이 아니거나 없거나 숫자가 아니면 400(서버 오류 500이 아님)")
    @Test
    void invalidPeriod() throws Exception {
        String token = token(admin);
        for (String period : new String[]{"14", "0", "seven", ""}) {
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
