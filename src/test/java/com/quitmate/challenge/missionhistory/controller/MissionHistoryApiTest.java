package com.quitmate.challenge.missionhistory.controller;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static com.quitmate.challenge.ChallengeFixture.deleteAll;
import static com.quitmate.challenge.ChallengeFixture.insertChallenge;
import static com.quitmate.challenge.ChallengeFixture.insertMission;
import static com.quitmate.challenge.ChallengeFixture.insertMissionHistory;
import static com.quitmate.challenge.ChallengeFixture.insertParticipation;
import static com.quitmate.challenge.ChallengeFixture.missionHistoryStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 SecurityConfig·JWT 필터로 미션 기록 관리자 API를 확인한다. 승인·반려(PATCH)는 KAN-194에서 없앴다.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class MissionHistoryApiTest {

    private static final String BASE = "/api/v1/admin/mission-history";
    private static final LocalDateTime STARTED = LocalDateTime.of(2026, 11, 2, 9, 0);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenGenerator jwtTokenGenerator;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        deleteAll(jdbcTemplate);
        userRepository.deleteAllInBatch();
    }

    @DisplayName("승인·반려 API가 없어 옛 승인 대기(READY) 기록을 바꾸지 못하고, 목록·상세 조회는 그대로 된다")
    @Test
    void judgeApiRemoved() throws Exception {
        // given
        User admin = userRepository.save(user("admin@test.com", Role.ADMIN));
        User member = userRepository.save(user("member@test.com", Role.USER));
        long challengeId = insertChallenge(jdbcTemplate, "금연 챌린지");
        long missionId = insertMission(jdbcTemplate, challengeId, MissionCategoryStatus.HOLD, "10분 참기", null);
        long participationId = insertParticipation(jdbcTemplate, challengeId, member.getId(),
                ChallengeStatus.PROGRESSING, STARTED, STARTED, null, null);
        long missionHistoryId = insertMissionHistory(jdbcTemplate, participationId, missionId, member.getId(),
                MissionStatus.READY);
        String token = token(admin);

        // when
        int judgeStatus = mockMvc.perform(patch(BASE + "/{id}", missionHistoryId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isSuccess\":true}"))
                .andReturn().getResponse().getStatus();

        // then
        assertThat(judgeStatus).as("승인 API는 없다").isGreaterThanOrEqualTo(400);
        assertThat(missionHistoryStatus(jdbcTemplate, missionHistoryId)).isEqualTo(MissionStatus.READY.name());

        mockMvc.perform(get(BASE).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].missionHistoryId").value(missionHistoryId))
                .andExpect(jsonPath("$.data.content[0].status").value("READY"));
        mockMvc.perform(get(BASE + "/{id}", missionHistoryId).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.category").value("HOLD"))
                .andExpect(jsonPath("$.data.abstinenceData.abstinenceTime").value(0));
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
