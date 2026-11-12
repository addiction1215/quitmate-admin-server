package com.quitmate.smokingPattern.controller;

import com.quitmate.global.jwt.JwtTokenGenerator;
import com.quitmate.global.jwt.dto.LoginUserInfo;
import com.quitmate.smokingPattern.enums.BaselineComparisonType;
import com.quitmate.smokingPattern.enums.RoutineType;
import com.quitmate.smokingPattern.enums.TrendDirection;
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
import java.util.List;

import static com.quitmate.smokingPattern.SmokingPatternFixture.createUserPassTableIfMissing;
import static com.quitmate.smokingPattern.SmokingPatternFixture.deleteAll;
import static com.quitmate.smokingPattern.SmokingPatternFixture.insertPass;
import static com.quitmate.smokingPattern.SmokingPatternFixture.insertSnapshot;
import static com.quitmate.smokingPattern.SmokingPatternFixture.okResult;
import static com.quitmate.smokingPattern.SmokingPatternFixture.relapse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 SecurityConfig·JWT 필터로 흡연 패턴 진단 관리자 API를 끝까지 확인한다.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class SmokingPatternApiTest {

    private static final String BASE = "/api/v1/admin/smoking-pattern";
    private static final LocalDate WEEK = LocalDate.of(2026, 11, 2);

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
        createUserPassTableIfMissing(jdbcTemplate);
        admin = userRepository.save(user("admin@test.com", Role.ADMIN));
        member = userRepository.save(user("member@test.com", Role.USER));
    }

    @AfterEach
    void tearDown() {
        deleteAll(jdbcTemplate);
        userRepository.deleteAllInBatch();
    }

    @DisplayName("토큰이 없으면 401, 관리자가 아니면 403")
    @Test
    void auth() throws Exception {
        for (String path : List.of("/weeks", "/snapshots", "/snapshots/1", "/users/1/snapshots", "/distribution")) {
            mockMvc.perform(get(BASE + path)).andExpect(status().isUnauthorized());
            mockMvc.perform(get(BASE + path).header("Authorization", token(member)))
                    .andExpect(status().isForbidden());
        }
    }

    @DisplayName("주 목록 → 그 주 사용자 목록 → 회원 주차별 목록 → 상세 → 전체 분포. 응답에 이메일은 없다")
    @Test
    void browse() throws Exception {
        // given
        long snapshotId = insertSnapshot(jdbcTemplate, member.getId(), okResult(WEEK, 8, TrendDirection.DECREASED,
                RoutineType.ROUTINE, BaselineComparisonType.LESS, 4, 1,
                List.of(relapse("2026-11-04", "WEDNESDAY", "08:15"))));
        insertPass(jdbcTemplate, member.getId(), WEEK.atStartOfDay(), WEEK.plusDays(90).atStartOfDay(), null);
        String token = token(admin);

        mockMvc.perform(get(BASE + "/weeks").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].periodStart").value("2026-11-02"))
                .andExpect(jsonPath("$.data[0].periodEnd").value("2026-11-08"))
                .andExpect(jsonPath("$.data[0].snapshotCount").value(1));

        mockMvc.perform(get(BASE + "/snapshots").param("periodStart", "2026-11-02").param("page", "1")
                        .param("size", "20").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(snapshotId))
                .andExpect(jsonPath("$.data.content[0].nickName").value("닉네임"))
                .andExpect(jsonPath("$.data.content[0].paid").value(true))
                .andExpect(jsonPath("$.data.content[0].dataSufficiency").value("SUFFICIENT"))
                .andExpect(jsonPath("$.data.content[0].email").doesNotExist())
                .andExpect(jsonPath("$.data.pageInfo.totalElement").value(1));

        mockMvc.perform(get(BASE + "/users/{userId}/snapshots", member.getId()).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].periodStart").value("2026-11-02"));

        mockMvc.perform(get(BASE + "/snapshots/{id}", snapshotId).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result.peakTime.peakHour").value(8))
                .andExpect(jsonPath("$.data.result.cravingResponse.smokedAfterCount").value(1))
                .andExpect(jsonPath("$.data.result.relapseMoments.moments[0].dayOfWeek").value("WEDNESDAY"))
                .andExpect(jsonPath("$.data.email").doesNotExist());

        mockMvc.perform(get(BASE + "/distribution").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.periodStart").value("2026-11-02"))
                .andExpect(jsonPath("$.data.all.snapshotCount").value(1))
                .andExpect(jsonPath("$.data.paid.snapshotCount").value(1))
                .andExpect(jsonPath("$.data.free.snapshotCount").value(0))
                .andExpect(jsonPath("$.data.paid.dataSufficiency.SUFFICIENT").value(1))
                .andExpect(jsonPath("$.data.paid.peakHour.hours[8]").value(1))
                .andExpect(jsonPath("$.data.paid.relapseMoments.byDayOfWeek.WEDNESDAY").value(1));
    }

    @DisplayName("월요일이 아닌 주 시작일은 400, 없는 스냅숏·회원은 404")
    @Test
    void errors() throws Exception {
        String token = token(admin);

        mockMvc.perform(get(BASE + "/distribution").param("periodStart", "2026-11-03").header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("월요일")));
        mockMvc.perform(get(BASE + "/snapshots/{id}", 999_999L).header("Authorization", token))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(BASE + "/users/{userId}/snapshots", 999_999L).header("Authorization", token))
                .andExpect(status().isNotFound());
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
