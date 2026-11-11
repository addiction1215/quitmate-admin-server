package com.quitmate.cigaretteHistoryBatch.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quitmate.cigaretteHistoryBatch.entity.CigaretteHistoryBatchIssue;
import com.quitmate.cigaretteHistoryBatch.enums.CigaretteHistoryBatchIssueStage;
import com.quitmate.cigaretteHistoryBatch.enums.CigaretteHistoryBatchIssueStatus;
import com.quitmate.cigaretteHistoryBatch.repository.CigaretteHistoryBatchIssueJpaRepository;
import com.quitmate.global.appserver.AppServerStub;
import com.quitmate.global.jwt.JwtTokenGenerator;
import com.quitmate.global.jwt.dto.LoginUserInfo;
import com.quitmate.user.users.entity.User;
import com.quitmate.user.users.entity.enums.Role;
import com.quitmate.user.users.entity.enums.SettingStatus;
import com.quitmate.user.users.entity.enums.Sex;
import com.quitmate.user.users.entity.enums.SnsType;
import com.quitmate.user.users.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 SecurityConfig·JWT 필터와 가짜 앱 서버로 「흡연 기록 이관」 관리자 API를 끝까지 확인한다.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class CigaretteHistoryBatchApiTest {

    private static final String BASE = "/api/v1/admin/cigarette-history-batch";
    /** 테스트 전용 앱 서버 서명 키(운영 키 아님) */
    private static final String APP_SERVER_TEST_KEY =
            "TD3ANZkMruvvCLpkQ/z++uiK7VMPV6eJMDj8fc7a9RawW03Z5vNQkg3Ko6fuTIiQ/cisBie3Hly56bzoPu+Ezw==";
    private static final AppServerStub APP_SERVER = new AppServerStub();

    @DynamicPropertySource
    static void appServer(DynamicPropertyRegistry registry) {
        registry.add("app-server.base-url", APP_SERVER::baseUrl);
        registry.add("app-server.jwt-secret-key", () -> APP_SERVER_TEST_KEY);
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtTokenGenerator jwtTokenGenerator;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CigaretteHistoryBatchIssueJpaRepository cigaretteHistoryBatchIssueJpaRepository;

    private User admin;
    private User user;

    @BeforeEach
    void setUp() {
        APP_SERVER.reset();
        admin = userRepository.save(user("admin@test.com"));
        user = user("user@test.com");
        user.updateRole(Role.USER);
        user = userRepository.save(user);
    }

    @AfterEach
    void tearDown() {
        cigaretteHistoryBatchIssueJpaRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @AfterAll
    static void stopAppServer() {
        APP_SERVER.close();
    }

    @DisplayName("토큰이 없으면 401")
    @Test
    void noToken() throws Exception {
        mockMvc.perform(get(BASE + "/issues"))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("관리자가 아닌 계정 토큰은 모든 API가 403이고 앱 서버를 부르지 않는다")
    @Test
    void notAdmin() throws Exception {
        String token = token(user);

        mockMvc.perform(get(BASE + "/issues").header("Authorization", token))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch(BASE + "/issues/{id}/ignore", 1L).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"memo\":\"메모\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/runs").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"targetDate\":\"20261105\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + "/runs").param("from", "20261101").param("to", "20261105")
                        .header("Authorization", token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + "/runs/{date}/attempts", "20261105").header("Authorization", token))
                .andExpect(status().isForbidden());

        assertThat(APP_SERVER.received()).isEmpty();
    }

    @DisplayName("관리자는 처리 대기 목록을 보고, 메모를 남겨 [무시]한다")
    @Test
    void listAndIgnore() throws Exception {
        // given
        CigaretteHistoryBatchIssue issue = cigaretteHistoryBatchIssueJpaRepository.save(pendingIssue());

        // when // then
        mockMvc.perform(get(BASE + "/issues").param("page", "1").param("size", "20")
                        .header("Authorization", token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(issue.getId()))
                .andExpect(jsonPath("$.data.content[0].targetDate").value("20261105"))
                .andExpect(jsonPath("$.data.content[0].userId").value(10))
                .andExpect(jsonPath("$.data.content[0].stage").value("SMOKED_SAVE"))
                .andExpect(jsonPath("$.data.content[0].failCount").value(3))
                .andExpect(jsonPath("$.data.pageInfo.totalElement").value(1));

        mockMvc.perform(patch(BASE + "/issues/{id}/ignore", issue.getId()).header("Authorization", token(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"memo\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("무시하는 이유(메모)는 필수입니다."));

        mockMvc.perform(patch(BASE + "/issues/{id}/ignore", issue.getId()).header("Authorization", token(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"memo\":\"원본 데이터 손상, 복구 안 함\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IGNORED"))
                .andExpect(jsonPath("$.data.memo").value("원본 데이터 손상, 복구 안 함"))
                .andExpect(jsonPath("$.data.resolvedBy").value(admin.getId()));

        mockMvc.perform(patch(BASE + "/issues/{id}/ignore", issue.getId()).header("Authorization", token(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"memo\":\"다시\"}"))
                .andExpect(status().isConflict());
        assertThat(APP_SERVER.received()).isEmpty();
    }

    @DisplayName("[재처리]는 앱 서버에 관리자 ID가 든 ADMIN 토큰과 force=false로 요청하고 결과를 돌려준다")
    @Test
    void run() throws Exception {
        // given
        APP_SERVER.respond(200, """
                {"statusCode":200,"httpStatus":"OK","message":"OK","data":{
                  "targetDate":"20261105","status":"DONE","attemptNo":4,"triggerType":"MANUAL",
                  "insertedUserCount":1,"skippedUserCount":52,"failedUserCount":0,
                  "startedAt":"2026-11-12T10:00:00.123456","finishedAt":"2026-11-12T10:00:02.5"}}
                """);

        // when // then
        mockMvc.perform(post(BASE + "/runs").header("Authorization", token(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"targetDate\":\"20261105\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DONE"))
                .andExpect(jsonPath("$.data.attemptNo").value(4))
                .andExpect(jsonPath("$.data.insertedUserCount").value(1))
                .andExpect(jsonPath("$.data.skippedUserCount").value(52))
                .andExpect(jsonPath("$.data.failedUserCount").value(0));

        AppServerStub.Received received = APP_SERVER.lastReceived();
        assertThat(received.method()).isEqualTo("POST");
        assertThat(received.uri()).isEqualTo("/api/v1/admin/cigarette-history-batch/runs");
        JsonNode body = objectMapper.readTree(received.body());
        assertThat(body.get("targetDate").asText()).isEqualTo("20261105");
        assertThat(body.get("force").asBoolean()).isFalse();
        JsonNode subject = objectMapper.readTree(Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(Decoders.BASE64.decode(APP_SERVER_TEST_KEY)))
                .build()
                .parseClaimsJws(received.authorization().substring("Bearer ".length()))
                .getBody()
                .getSubject());
        assertThat(subject.get("userId").asLong()).isEqualTo(admin.getId());
        assertThat(subject.get("role").asText()).isEqualTo("ADMIN");
    }

    @DisplayName("앱 서버가 409·400을 주면 같은 상태와 사유를 화면에 돌려준다")
    @Test
    void runRejected() throws Exception {
        APP_SERVER.respond(409, "{\"statusCode\":409,\"httpStatus\":\"CONFLICT\","
                + "\"message\":\"다른 실행이 이 날짜를 처리하고 있습니다. 잠시 후 다시 시도해 주세요.\"}");
        mockMvc.perform(post(BASE + "/runs").header("Authorization", token(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"targetDate\":\"20261105\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("다른 실행이 이 날짜를 처리하고 있습니다. 잠시 후 다시 시도해 주세요."));

        APP_SERVER.respond(400, "{\"statusCode\":400,\"httpStatus\":\"BAD_REQUEST\","
                + "\"message\":\"오늘이나 미래 날짜는 이관할 수 없습니다. 어제 날짜까지만 다시 처리할 수 있습니다.\"}");
        mockMvc.perform(post(BASE + "/runs").header("Authorization", token(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"targetDate\":\"20991231\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("오늘이나 미래 날짜는 이관할 수 없습니다. 어제 날짜까지만 다시 처리할 수 있습니다."));

        // 앱 서버가 토큰을 거절해도 관리자 화면이 로그아웃되지 않게 401이 아닌 502로 준다.
        APP_SERVER.respond(401, "{\"statusCode\":401,\"message\":\"JWT access token이 만료되었습니다.\"}");
        mockMvc.perform(post(BASE + "/runs").header("Authorization", token(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"targetDate\":\"20261105\"}"))
                .andExpect(status().isBadGateway());
    }

    @DisplayName("날짜 형식이 틀리면 앱 서버를 부르지 않고 400")
    @Test
    void runBadDate() throws Exception {
        mockMvc.perform(post(BASE + "/runs").header("Authorization", token(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"targetDate\":\"2026-11-05\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이관할 날짜는 yyyyMMdd 형식이어야 합니다. 예: 20261105"));
        mockMvc.perform(get(BASE + "/runs/{date}/attempts", "2026115").header("Authorization", token(admin)))
                .andExpect(status().isBadRequest());

        assertThat(APP_SERVER.received()).isEmpty();
    }

    @DisplayName("날짜별 상태와 실행 이력은 앱 서버 응답을 그대로 보여 준다")
    @Test
    void statusesAndAttempts() throws Exception {
        APP_SERVER.respond(200, """
                {"statusCode":200,"message":"OK","data":[
                  {"targetDate":"20261104","status":null,"attemptCount":0},
                  {"targetDate":"20261105","status":"FAILED","attemptCount":3,"lastTriggerType":"CATCH_UP",
                   "totalUserCount":53,"successUserCount":52,"failedUserCount":1,"lastErrorSummary":"사용자 저장 실패 1명",
                   "startedAt":"2026-11-08T00:00:00","finishedAt":"2026-11-08T00:00:05"}]}
                """);
        mockMvc.perform(get(BASE + "/runs").param("from", "20261104").param("to", "20261105")
                        .header("Authorization", token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].targetDate").value("20261104"))
                .andExpect(jsonPath("$.data[0].status").doesNotExist())
                .andExpect(jsonPath("$.data[1].status").value("FAILED"))
                .andExpect(jsonPath("$.data[1].failedUserCount").value(1))
                .andExpect(jsonPath("$.data[1].lastErrorSummary").value("사용자 저장 실패 1명"));
        assertThat(APP_SERVER.lastReceived().uri())
                .isEqualTo("/api/v1/admin/cigarette-history-batch/runs?from=20261104&to=20261105");

        APP_SERVER.respond(200, """
                {"statusCode":200,"message":"OK","data":[
                  {"attemptNo":1,"triggerType":"SCHEDULED","requestedBy":null,"executor":"ip-10-0-1-5:1234:a1b2c3d4",
                   "activeProfile":"prd","status":"FAILED","startedAt":"2026-11-06T00:00:00",
                   "finishedAt":"2026-11-06T00:00:04","sourceRowCount":120,"totalUserCount":53,
                   "insertedUserCount":52,"skippedUserCount":0,"failedUserCount":1,"retriedUserCount":0,
                   "deletedSourceRowCount":118,"errorType":null,"errorMessage":null}]}
                """);
        mockMvc.perform(get(BASE + "/runs/{date}/attempts", "20261105").header("Authorization", token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].attemptNo").value(1))
                .andExpect(jsonPath("$.data[0].triggerType").value("SCHEDULED"))
                .andExpect(jsonPath("$.data[0].executor").value("ip-10-0-1-5:1234:a1b2c3d4"))
                .andExpect(jsonPath("$.data[0].deletedSourceRowCount").value(118));
        assertThat(APP_SERVER.lastReceived().uri())
                .isEqualTo("/api/v1/admin/cigarette-history-batch/runs/20261105/attempts");
    }

    private String token(User account) throws Exception {
        return "Bearer " + jwtTokenGenerator.generate(LoginUserInfo.of(account.getId())).getAccessToken();
    }

    private static User user(String email) {
        return User.builder()
                .email(email)
                .password("encoded")
                .nickName("닉네임")
                .phoneNumber("010-0000-0000")
                .sex(Sex.MALE)
                .role(Role.ADMIN)
                .snsType(SnsType.NORMAL)
                .settingStatus(SettingStatus.COMPLETE)
                .build();
    }

    private static CigaretteHistoryBatchIssue pendingIssue() {
        LocalDateTime failedAt = LocalDateTime.of(2026, 11, 6, 0, 0);
        return CigaretteHistoryBatchIssue.builder()
                .targetDate("20261105")
                .userId(10L)
                .stage(CigaretteHistoryBatchIssueStage.SMOKED_SAVE)
                .failCount(3)
                .status(CigaretteHistoryBatchIssueStatus.PENDING)
                .firstFailedAt(failedAt)
                .lastFailedAt(failedAt.plusDays(2))
                .lastErrorType("com.mongodb.MongoException")
                .build();
    }
}
