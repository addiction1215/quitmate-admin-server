package com.quitmate.challenge.challange.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.quitmate.challenge.ChallengeFixture.deleteAll;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 SecurityConfig·검증·Jackson으로 미션 최소 참기 시간(KAN-194) 등록·수정·조회를 확인한다.
 * 수정 요청에서 칸을 "생략"하는 것과 "null"을 보내는 것을 구분해야 해서 JSON 문자열을 직접 보낸다.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ChallengeMinHoldSecondsApiTest {

    private static final String BASE = "/api/v1/admin/challenge";
    private static final String RANGE_MESSAGE = "최소 참기 시간은 60초(1분)부터 2592000초(30일)까지 넣을 수 있습니다.";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtTokenGenerator jwtTokenGenerator;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        User admin = userRepository.save(User.builder()
                .email("admin@test.com")
                .password("encoded")
                .nickName("관리자")
                .phoneNumber("010-0000-0000")
                .sex(Sex.MALE)
                .role(Role.ADMIN)
                .snsType(SnsType.NORMAL)
                .settingStatus(SettingStatus.COMPLETE)
                .build());
        token = "Bearer " + jwtTokenGenerator.generate(LoginUserInfo.of(admin.getId())).getAccessToken();
    }

    @AfterEach
    void tearDown() {
        deleteAll(jdbcTemplate);
        userRepository.deleteAllInBatch();
    }

    @DisplayName("등록: 참기 미션만 최소 참기 시간을 저장하고, 비우면 null(앱 서버 기본값), 상세 응답에 그대로 나온다")
    @Test
    void create() throws Exception {
        // when
        long challengeId = create("""
                [
                  {"category":"HOLD","title":"5분 참기","reward":10,"content":"참기","minHoldSeconds":300},
                  {"category":"HOLD","title":"기본 참기","reward":10,"content":"참기"},
                  {"category":"LOCATION","title":"위치","reward":10,"content":"위치","minHoldSeconds":300}
                ]""");

        // then
        assertThat(minHoldSeconds(challengeId)).containsEntry("5분 참기", 300)
                .containsEntry("기본 참기", null)
                .containsEntry("위치", null);
        mockMvc.perform(get(BASE + "/{id}", challengeId).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.missions[?(@.title == '5분 참기')].minHoldSeconds").value(contains(300)))
                .andExpect(jsonPath("$.data.missions[?(@.title == '기본 참기')].minHoldSeconds").value(contains(nullValue())))
                .andExpect(jsonPath("$.data.missions[?(@.title == '위치')].minHoldSeconds").value(contains(nullValue())));
    }

    @DisplayName("등록·수정: 60초 미만이나 2592000초(30일) 초과는 사람이 읽는 문장으로 400")
    @Test
    void outOfRange() throws Exception {
        for (int seconds : List.of(59, 2_592_001, 0, -1)) {
            mockMvc.perform(post(BASE).header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(challengeJson("[{\"category\":\"HOLD\",\"title\":\"참기\",\"reward\":10,"
                                    + "\"content\":\"참기\",\"minHoldSeconds\":" + seconds + "}]")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(RANGE_MESSAGE));
        }
        long challengeId = create("[{\"category\":\"HOLD\",\"title\":\"참기\",\"reward\":10,\"content\":\"참기\",\"minHoldSeconds\":60}]");
        long missionId = missionId(challengeId, "참기");
        mockMvc.perform(put(BASE + "/{id}", challengeId).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(challengeJson("[{\"missionId\":" + missionId + ",\"category\":\"HOLD\",\"title\":\"참기\","
                                + "\"reward\":10,\"content\":\"참기\",\"minHoldSeconds\":2592001}]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(RANGE_MESSAGE));

        assertThat(minHoldSeconds(challengeId)).containsEntry("참기", 60);
    }

    @DisplayName("등록: 하루가 넘는 최소 시간(홀수 챌린지 9일 참기 = 777600초)과 상한 30일을 받는다")
    @Test
    void dayLongMinHold() throws Exception {
        long challengeId = create("[{\"category\":\"HOLD\",\"title\":\"9일 참기\",\"reward\":10,\"content\":\"참기\","
                + "\"minHoldSeconds\":777600},{\"category\":\"HOLD\",\"title\":\"30일 참기\",\"reward\":10,"
                + "\"content\":\"참기\",\"minHoldSeconds\":2592000}]");

        assertThat(minHoldSeconds(challengeId))
                .containsEntry("9일 참기", 777_600)
                .containsEntry("30일 참기", 2_592_000);
    }

    @DisplayName("수정: 칸을 생략하면 그대로 두고, null이면 기본값으로, 숫자면 바꾸고, 참기가 아닌 종류로 바꾸면 비운다")
    @Test
    void update() throws Exception {
        // given
        long challengeId = create("""
                [
                  {"category":"HOLD","title":"생략","reward":10,"content":"참기","minHoldSeconds":300},
                  {"category":"HOLD","title":"null","reward":10,"content":"참기","minHoldSeconds":300},
                  {"category":"HOLD","title":"변경","reward":10,"content":"참기","minHoldSeconds":300},
                  {"category":"HOLD","title":"종류 변경","reward":10,"content":"참기","minHoldSeconds":300}
                ]""");

        // when: KAN-195 전 관리자 웹처럼 minHoldSeconds 칸 없이 보내는 미션을 섞는다
        mockMvc.perform(put(BASE + "/{id}", challengeId).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(challengeJson("[" + String.join(",",
                                "{\"missionId\":" + missionId(challengeId, "생략") + ",\"category\":\"HOLD\",\"title\":\"생략\",\"reward\":10,\"content\":\"참기\"}",
                                "{\"missionId\":" + missionId(challengeId, "null") + ",\"category\":\"HOLD\",\"title\":\"null\",\"reward\":10,\"content\":\"참기\",\"minHoldSeconds\":null}",
                                "{\"missionId\":" + missionId(challengeId, "변경") + ",\"category\":\"HOLD\",\"title\":\"변경\",\"reward\":10,\"content\":\"참기\",\"minHoldSeconds\":900}",
                                "{\"missionId\":" + missionId(challengeId, "종류 변경") + ",\"category\":\"LOCATION\",\"title\":\"종류 변경\",\"reward\":10,\"content\":\"위치\",\"minHoldSeconds\":900}",
                                "{\"category\":\"HOLD\",\"title\":\"새 미션\",\"reward\":10,\"content\":\"참기\"}"
                        ) + "]")))
                .andExpect(status().isNoContent());

        // then
        assertThat(minHoldSeconds(challengeId))
                .containsEntry("생략", 300)
                .containsEntry("null", null)
                .containsEntry("변경", 900)
                .containsEntry("종류 변경", null)
                .containsEntry("새 미션", null);
    }

    private long create(String missionsJson) throws Exception {
        String body = mockMvc.perform(post(BASE).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(challengeJson(missionsJson)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(body).path("data");
        return data.path("challengeId").asLong();
    }

    private static String challengeJson(String missionsJson) {
        return "{\"title\":\"참기 챌린지\",\"badge\":\"badge/hold\",\"content\":\"설명\",\"reward\":40,\"missions\":"
                + missionsJson + "}";
    }

    private long missionId(long challengeId, String title) {
        return jdbcTemplate.queryForObject("select id from mission where challenge_id = ? and title = ? and use_yn = 'Y'",
                Long.class, challengeId, title);
    }

    /** 사용 중 미션의 제목 → min_hold_seconds */
    private Map<String, Integer> minHoldSeconds(long challengeId) {
        Map<String, Integer> result = new HashMap<>();
        jdbcTemplate.query("select title, min_hold_seconds from mission where challenge_id = ? and use_yn = 'Y'",
                rs -> {
                    Object value = rs.getObject("min_hold_seconds");
                    result.put(rs.getString("title"), value == null ? null : ((Number) value).intValue());
                }, challengeId);
        return result;
    }
}
