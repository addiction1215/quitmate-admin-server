package com.quitmate.appMetrics.ga4.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class Ga4DataApiClientTest {

    private static final String PROPERTY_ID = "123456789";
    private static final Ga4ReportRequest TOP_SCREENS = new Ga4ReportRequest("topScreens", "28daysAgo", "yesterday",
            List.of("unifiedScreenName"), List.of("screenPageViews"), "screenPageViews", 10);
    private static final Ga4ReportRequest SUMMARY = new Ga4ReportRequest("summary", "7daysAgo", "yesterday",
            List.of(), List.of("active1DayUsers", "active7DayUsers"), null, null);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Ga4AccessTokenProvider tokenProvider = mock(Ga4AccessTokenProvider.class);
    private final Ga4ApiStub stub = new Ga4ApiStub();

    @BeforeEach
    void setUp() {
        given(tokenProvider.accessToken()).willReturn("fake-access-token");
    }

    @AfterEach
    void tearDown() {
        stub.close();
    }

    @DisplayName("runReport를 Bearer 토큰과 JSON 본문으로 보내고, 행·보고 시간대를 읽는다")
    @Test
    void runReport() throws Exception {
        // given
        stub.respond(200, """
                {"dimensionHeaders":[{"name":"unifiedScreenName"}],
                 "metricHeaders":[{"name":"screenPageViews","type":"TYPE_INTEGER"}],
                 "rows":[{"dimensionValues":[{"value":"Home"}],"metricValues":[{"value":"1234"}]},
                         {"dimensionValues":[{"value":"Challenge"}],"metricValues":[{"value":"56.6"}]}],
                 "rowCount":2,"metadata":{"currencyCode":"KRW","timeZone":"Asia/Seoul"},"kind":"analyticsData#runReport"}
                """);

        // when
        Ga4Report report = client(Duration.ofSeconds(2)).runReport(PROPERTY_ID, TOP_SCREENS);

        // then
        assertThat(report.timeZone()).isEqualTo("Asia/Seoul");
        assertThat(report.rows()).hasSize(2);
        assertThat(report.rows().get(0).dimension(0)).isEqualTo("Home");
        assertThat(report.rows().get(0).metric(0)).isEqualTo(1234L);
        assertThat(report.rows().get(1).metric(0)).isEqualTo(57L);

        Ga4ApiStub.Received received = stub.lastReceived();
        assertThat(received.method()).isEqualTo("POST");
        assertThat(received.uri()).isEqualTo("/v1beta/properties/123456789:runReport");
        assertThat(received.authorization()).isEqualTo("Bearer fake-access-token");
        assertThat(received.contentType()).startsWith("application/json");
        JsonNode body = objectMapper.readTree(received.body());
        assertThat(body.at("/dateRanges/0/startDate").asText()).isEqualTo("28daysAgo");
        assertThat(body.at("/dateRanges/0/endDate").asText()).isEqualTo("yesterday");
        assertThat(body.at("/dimensions/0/name").asText()).isEqualTo("unifiedScreenName");
        assertThat(body.at("/metrics/0/name").asText()).isEqualTo("screenPageViews");
        assertThat(body.at("/orderBys/0/metric/metricName").asText()).isEqualTo("screenPageViews");
        assertThat(body.at("/orderBys/0/desc").asBoolean()).isTrue();
        assertThat(body.get("limit").asInt()).isEqualTo(10);
        assertThat(body.has("name")).isFalse();
    }

    @DisplayName("차원이 없으면 dimensions·orderBys·limit을 보내지 않고, 행이 없으면 빈 목록이다")
    @Test
    void summaryWithoutRows() throws Exception {
        stub.respond(200, "{\"metricHeaders\":[{\"name\":\"active1DayUsers\"}],\"metadata\":{\"timeZone\":\"America/Los_Angeles\"}}");

        Ga4Report report = client(Duration.ofSeconds(2)).runReport(PROPERTY_ID, SUMMARY);

        assertThat(report.rows()).isEmpty();
        assertThat(report.timeZone()).isEqualTo("America/Los_Angeles");
        JsonNode body = objectMapper.readTree(stub.lastReceived().body());
        assertThat(body.has("dimensions")).isFalse();
        assertThat(body.has("orderBys")).isFalse();
        assertThat(body.has("limit")).isFalse();
        assertThat(body.at("/metrics/1/name").asText()).isEqualTo("active7DayUsers");
    }

    @DisplayName("GA4 오류 응답(권한·쿼터)은 503 예외. 사유에는 상태만 넣고 응답 본문 문구·속성 ID·토큰은 넣지 않는다")
    @Test
    void errorResponse() {
        stub.respond(403, """
                {"error":{"code":403,"message":"User does not have sufficient permissions for this property 123456789.","status":"PERMISSION_DENIED"}}
                """);
        Ga4DataApiClient client = client(Duration.ofSeconds(2));

        assertThatThrownBy(() -> client.runReport(PROPERTY_ID, TOP_SCREENS))
                .isInstanceOf(Ga4UnavailableException.class)
                .hasMessageContaining("403 PERMISSION_DENIED")
                .hasMessageContaining("topScreens")
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain(PROPERTY_ID, "permissions", "fake-access-token"));

        stub.respond(429, "{\"error\":{\"code\":429,\"status\":\"RESOURCE_EXHAUSTED\"}}");
        assertThatThrownBy(() -> client.runReport(PROPERTY_ID, TOP_SCREENS))
                .isInstanceOf(Ga4UnavailableException.class)
                .hasMessageContaining("429 RESOURCE_EXHAUSTED");

        stub.respond(500, "<html>oops</html>");
        assertThatThrownBy(() -> client.runReport(PROPERTY_ID, TOP_SCREENS))
                .isInstanceOf(Ga4UnavailableException.class)
                .hasMessageContaining("500 UNKNOWN");
    }

    @DisplayName("응답 이상(JSON 아님, 지표 개수 다름, 숫자 아님)도 503 예외")
    @Test
    void malformedResponse() {
        Ga4DataApiClient client = client(Duration.ofSeconds(2));

        stub.respond(200, "not-json");
        assertThatThrownBy(() -> client.runReport(PROPERTY_ID, TOP_SCREENS)).isInstanceOf(Ga4UnavailableException.class);

        stub.respond(200, "{\"rows\":[{\"dimensionValues\":[{\"value\":\"Home\"}],\"metricValues\":[]}]}");
        assertThatThrownBy(() -> client.runReport(PROPERTY_ID, TOP_SCREENS))
                .isInstanceOf(Ga4UnavailableException.class)
                .hasMessageContaining("개수");

        stub.respond(200, "{\"rows\":[{\"dimensionValues\":[{\"value\":\"Home\"}],\"metricValues\":[{\"value\":\"many\"}]}]}");
        assertThatThrownBy(() -> client.runReport(PROPERTY_ID, TOP_SCREENS))
                .isInstanceOf(Ga4UnavailableException.class)
                .hasMessageContaining("숫자");

        stub.respond(200, "[]");
        assertThatThrownBy(() -> client.runReport(PROPERTY_ID, TOP_SCREENS)).isInstanceOf(Ga4UnavailableException.class);
    }

    @DisplayName("연결 실패·응답 시간 초과도 503 예외")
    @Test
    void networkFailure() throws Exception {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        Ga4DataApiClient unreachable = new Ga4DataApiClient(tokenProvider, objectMapper, RestClient.builder(),
                "http://127.0.0.1:" + closedPort, Duration.ofSeconds(1), Duration.ofSeconds(1));
        assertThatThrownBy(() -> unreachable.runReport(PROPERTY_ID, TOP_SCREENS))
                .isInstanceOf(Ga4UnavailableException.class)
                .hasMessageContaining("부르지 못했습니다");

        stub.respond(200, "{}");
        stub.respondSlowly(1_000);
        assertThatThrownBy(() -> client(Duration.ofMillis(200)).runReport(PROPERTY_ID, TOP_SCREENS))
                .isInstanceOf(Ga4UnavailableException.class)
                .hasMessageContaining("SocketTimeoutException");
    }

    @DisplayName("토큰을 받지 못하면 GA4를 부르지 않고 그 예외를 그대로 던진다. 속성 ID가 숫자가 아니어도 부르지 않는다")
    @Test
    void noCallWithoutTokenOrValidPropertyId() {
        given(tokenProvider.accessToken()).willThrow(new Ga4UnavailableException("GA4 토큰을 받지 못했습니다(IOException)."));
        Ga4DataApiClient client = client(Duration.ofSeconds(2));

        assertThatThrownBy(() -> client.runReport(PROPERTY_ID, TOP_SCREENS))
                .isInstanceOf(Ga4UnavailableException.class)
                .hasMessageContaining("토큰");
        assertThatThrownBy(() -> client.runReport("12/../34", TOP_SCREENS))
                .isInstanceOf(Ga4UnavailableException.class)
                .hasMessageContaining("속성 ID");
        assertThat(stub.received()).isEmpty();
    }

    private Ga4DataApiClient client(Duration readTimeout) {
        return new Ga4DataApiClient(tokenProvider, objectMapper, RestClient.builder(), stub.baseUrl(),
                Duration.ofSeconds(1), readTimeout);
    }
}
