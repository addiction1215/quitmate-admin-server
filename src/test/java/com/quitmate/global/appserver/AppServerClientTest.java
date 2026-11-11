package com.quitmate.global.appserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.quitmate.cigaretteHistoryBatch.service.response.CigaretteHistoryBatchRunResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

import java.net.ServerSocket;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import static com.quitmate.global.appserver.AppServerTokenIssuerTest.BASE64_TEST_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppServerClientTest {

    private static final String RUNS = "/api/v1/admin/cigarette-history-batch/runs";
    private static final ParameterizedTypeReference<AppServerResponse<CigaretteHistoryBatchRunResponse>> RUN_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final AppServerStub stub = new AppServerStub();

    @AfterEach
    void tearDown() {
        stub.close();
    }

    @DisplayName("앱 서버에 관리자 토큰(Bearer)과 본문을 보내고, 응답의 data를 돌려준다")
    @Test
    void post() throws Exception {
        // given
        stub.respond(200, """
                {"statusCode":200,"httpStatus":"OK","message":"OK","data":{
                  "targetDate":"20261105","status":"DONE","attemptNo":2,"triggerType":"MANUAL",
                  "insertedUserCount":1,"skippedUserCount":52,"failedUserCount":0,
                  "startedAt":"2026-11-06T10:00:00.123456","finishedAt":"2026-11-06T10:00:02"}}
                """);
        AppServerClient client = client(stub.baseUrl(), Duration.ofSeconds(2));

        // when
        CigaretteHistoryBatchRunResponse response = client.post(RUNS, Map.of("targetDate", "20261105", "force", false),
                RUN_TYPE, 9L);

        // then
        assertThat(response.getStatus()).isEqualTo("DONE");
        assertThat(response.getAttemptNo()).isEqualTo(2);
        assertThat(response.getSkippedUserCount()).isEqualTo(52);
        assertThat(response.getFinishedAt()).isEqualTo(LocalDateTime.of(2026, 11, 6, 10, 0, 2));

        AppServerStub.Received received = stub.lastReceived();
        assertThat(received.method()).isEqualTo("POST");
        assertThat(received.uri()).isEqualTo(RUNS);
        JsonNode body = objectMapper.readTree(received.body());
        assertThat(body.get("targetDate").asText()).isEqualTo("20261105");
        assertThat(body.get("force").asBoolean()).isFalse();

        assertThat(received.authorization()).startsWith("Bearer ");
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(Decoders.BASE64.decode(BASE64_TEST_KEY)))
                .build()
                .parseClaimsJws(received.authorization().substring("Bearer ".length()))
                .getBody();
        assertThat(claims.get("tokenType", String.class)).isEqualTo("ACCESS");
        assertThat(objectMapper.readTree(claims.getSubject()).get("userId").asLong()).isEqualTo(9L);
        assertThat(objectMapper.readTree(claims.getSubject()).get("role").asText()).isEqualTo("ADMIN");
    }

    @DisplayName("쿼리 파라미터를 붙여 GET한다")
    @Test
    void get() {
        // given
        stub.respond(200, "{\"statusCode\":200,\"message\":\"OK\",\"data\":[]}");
        AppServerClient client = client(stub.baseUrl() + "/", Duration.ofSeconds(2));

        // when
        client.get(RUNS, Map.of("from", "20261101"), new ParameterizedTypeReference<AppServerResponse<Object>>() {
        }, 1L);

        // then
        assertThat(stub.lastReceived().method()).isEqualTo("GET");
        assertThat(stub.lastReceived().uri()).isEqualTo(RUNS + "?from=20261101");
    }

    @DisplayName("앱 서버의 409·400은 상태와 사유를 그대로 넘긴다")
    @Test
    void conflictAndBadRequest() {
        AppServerClient client = client(stub.baseUrl(), Duration.ofSeconds(2));

        stub.respond(409, "{\"statusCode\":409,\"httpStatus\":\"CONFLICT\",\"message\":\"이미 이관이 끝난 날짜입니다.\"}");
        assertThatThrownBy(() -> client.post(RUNS, Map.of(), RUN_TYPE, 1L))
                .isInstanceOfSatisfying(AppServerException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getMessage()).isEqualTo("이미 이관이 끝난 날짜입니다.");
                });

        stub.respond(400, "{\"statusCode\":400,\"message\":\"오늘이나 미래 날짜는 이관할 수 없습니다.\"}");
        assertThatThrownBy(() -> client.post(RUNS, Map.of(), RUN_TYPE, 1L))
                .isInstanceOfSatisfying(AppServerException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getMessage()).isEqualTo("오늘이나 미래 날짜는 이관할 수 없습니다.");
                });
    }

    @DisplayName("앱 서버의 401·403·500은 관리자 화면이 로그아웃되지 않게 502로 바꾼다")
    @Test
    void authAndServerErrorsBecomeBadGateway() {
        AppServerClient client = client(stub.baseUrl(), Duration.ofSeconds(2));

        for (int status : new int[]{401, 403, 404, 500}) {
            stub.respond(status, "{\"message\":\"x\"}");
            assertThatThrownBy(() -> client.post(RUNS, Map.of(), RUN_TYPE, 1L))
                    .isInstanceOfSatisfying(AppServerException.class,
                            e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
        }
    }

    @DisplayName("앱 서버에 연결하지 못하면 502")
    @Test
    void connectionRefused() throws Exception {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        AppServerClient client = client("http://127.0.0.1:" + closedPort, Duration.ofSeconds(2));

        assertThatThrownBy(() -> client.post(RUNS, Map.of(), RUN_TYPE, 1L))
                .isInstanceOfSatisfying(AppServerException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @DisplayName("재처리 응답이 제한 시간을 넘기면 504와 '처리 중일 수 있음' 안내")
    @Test
    void readTimeout() {
        stub.respond(200, "{\"data\":{}}");
        stub.respondSlowly(1_500);
        AppServerClient client = client(stub.baseUrl(), Duration.ofMillis(300));

        assertThatThrownBy(() -> client.post(RUNS, Map.of(), RUN_TYPE, 1L))
                .isInstanceOfSatisfying(AppServerException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
                    assertThat(e.getMessage()).contains("처리 중일 수 있으니");
                });
    }

    @DisplayName("앱 서버 주소가 없으면 부르지 않고 503")
    @Test
    void missingBaseUrl() {
        for (String missing : new String[]{null, "", "null"}) {
            AppServerClient client = client(missing, Duration.ofSeconds(2));
            assertThatThrownBy(() -> client.post(RUNS, Map.of(), RUN_TYPE, 1L))
                    .isInstanceOfSatisfying(AppServerException.class,
                            e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        }
        assertThat(stub.received()).isEmpty();
    }

    private AppServerClient client(String baseUrl, Duration readTimeout) {
        AppServerProperties properties = new AppServerProperties(baseUrl, BASE64_TEST_KEY, Duration.ofMinutes(5),
                Duration.ofSeconds(1), readTimeout);
        return new AppServerClient(properties, new AppServerTokenIssuer(properties, objectMapper), objectMapper,
                RestClient.builder());
    }
}
