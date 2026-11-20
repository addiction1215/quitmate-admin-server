package com.quitmate.challenge.metrics.controller;

import com.quitmate.challenge.challengehistory.entity.ChallengeStatus;
import com.quitmate.challenge.mission.entity.MissionCategoryStatus;
import com.quitmate.challenge.missionhistory.entity.MissionStatus;
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

import java.time.LocalDateTime;
import java.util.List;

import static com.quitmate.challenge.ChallengeFixture.deleteAll;
import static com.quitmate.challenge.ChallengeFixture.insertChallenge;
import static com.quitmate.challenge.ChallengeFixture.insertMission;
import static com.quitmate.challenge.ChallengeFixture.insertMissionHistory;
import static com.quitmate.challenge.ChallengeFixture.insertParticipation;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 SecurityConfig·JWT 필터·Jackson으로 챌린지 지표 API(KAN-194)의 권한·경로·JSON 모양을 확인한다.
 * 계산 자체는 ChallengeMetricsReadServiceTest·ChallengeMetricsCalculatorTest가 본다.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ChallengeMetricsApiTest {

    private static final String BASE = "/api/v1/admin/challenge";
    private static final LocalDateTime STARTED = LocalDateTime.of(2026, 11, 2, 9, 0);

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
    private long challengeId;
    private long missionId;

    @BeforeEach
    void setUp() {
        admin = userRepository.save(user("admin@test.com", Role.ADMIN));
        member = userRepository.save(user("member@test.com", Role.USER));
        challengeId = insertChallenge(jdbcTemplate, "출근길 금연");
        missionId = insertMission(jdbcTemplate, challengeId, MissionCategoryStatus.HOLD, "10분 참기", 300);
        long participationId = insertParticipation(jdbcTemplate, challengeId, member.getId(), ChallengeStatus.COMPLETED,
                STARTED, STARTED.plusMinutes(90), STARTED.plusMinutes(90), null);
        insertMissionHistory(jdbcTemplate, participationId, missionId, member.getId(), MissionStatus.COMPLETED);
    }

    @AfterEach
    void tearDown() {
        deleteAll(jdbcTemplate);
        userRepository.deleteAllInBatch();
    }

    @DisplayName("토큰이 없으면 401, 관리자가 아니면 403")
    @Test
    void auth() throws Exception {
        for (String path : List.of(BASE + "/metrics", BASE + "/" + challengeId + "/metrics")) {
            mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
            mockMvc.perform(get(path).header("Authorization", token(member))).andExpect(status().isForbidden());
        }
    }

    @DisplayName("목록·상세 지표의 JSON 모양: 분자·분모·퍼센트와 기간을 함께 준다. /metrics는 챌린지 상세(/{id})와 겹치지 않는다")
    @Test
    void shape() throws Exception {
        String token = token(admin);

        mockMvc.perform(get(BASE + "/metrics").param("from", "2026-11-01").param("to", "2026-11-07")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.from").value("2026-11-01"))
                .andExpect(jsonPath("$.data.to").value("2026-11-07"))
                .andExpect(jsonPath("$.data.challenges[0].challengeId").value(challengeId))
                .andExpect(jsonPath("$.data.challenges[0].title").value("출근길 금연"))
                .andExpect(jsonPath("$.data.challenges[0].participantCount").value(1))
                .andExpect(jsonPath("$.data.challenges[0].participationCount").value(1))
                .andExpect(jsonPath("$.data.challenges[0].completedCount").value(1))
                .andExpect(jsonPath("$.data.challenges[0].completion.numerator").value(1))
                .andExpect(jsonPath("$.data.challenges[0].completion.denominator").value(1))
                .andExpect(jsonPath("$.data.challenges[0].completion.percent").value(100))
                .andExpect(jsonPath("$.data.challenges[0].cancellation.percent").value(0))
                .andExpect(jsonPath("$.data.challenges[0].rejoin.denominator").value(0))
                .andExpect(jsonPath("$.data.challenges[0].rejoin.percent").value(nullValue()))
                .andExpect(jsonPath("$.data.challenges[0].averageHoursToComplete").value(1.5))
                .andExpect(jsonPath("$.data.challenges[0].averageHoursToCancel").value(nullValue()))
                .andExpect(jsonPath("$.data.challenges[0].endTimeFallbackCount").value(0))
                .andExpect(jsonPath("$.data.challenges[0].missions").doesNotExist());

        mockMvc.perform(get(BASE + "/{id}/metrics", challengeId).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.from").value(nullValue()))
                .andExpect(jsonPath("$.data.challenge.challengeId").value(challengeId))
                .andExpect(jsonPath("$.data.challenge.completion.percent").value(100))
                .andExpect(jsonPath("$.data.missions[0].missionId").value(missionId))
                .andExpect(jsonPath("$.data.missions[0].title").value("10분 참기"))
                .andExpect(jsonPath("$.data.missions[0].category").value("HOLD"))
                .andExpect(jsonPath("$.data.missions[0].completion.numerator").value(1))
                .andExpect(jsonPath("$.data.missions[0].completion.denominator").value(1))
                .andExpect(jsonPath("$.data.missions[0].completion.percent").value(100));

        // 챌린지 상세는 그대로
        mockMvc.perform(get(BASE + "/{id}", challengeId).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.missions[0].minHoldSeconds").value(300));
    }

    @DisplayName("기간이 틀리면 사람이 읽는 문장으로 400")
    @Test
    void invalidPeriod() throws Exception {
        String token = token(admin);

        mockMvc.perform(get(BASE + "/metrics").param("from", "11월 1일").header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("기간은 2026-11-01 형식(yyyy-MM-dd)으로 보내 주세요."));
        mockMvc.perform(get(BASE + "/{id}/metrics", challengeId).param("from", "2026-11-08").param("to", "2026-11-07")
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("기간 시작일(from)은 끝 날짜(to)보다 늦을 수 없습니다."));
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
