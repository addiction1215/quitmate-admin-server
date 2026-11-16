package com.quitmate.global.interceptor;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingInterceptorTest {

    private static final String R = LoggingInterceptor.REDACTED;

    private final LoggingInterceptor interceptor = new LoggingInterceptor(new ObjectMapper());

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(LoggingInterceptor.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    @DisplayName("로그인 경로는 요청·응답 본문을 통째로 가린다")
    @Test
    void loginPathIsRedacted() {
        String request = "{\"email\":\"admin@quitmate.co.kr\",\"password\":\"secret-password\"}";
        String response = "{\"data\":{\"email\":\"admin@quitmate.co.kr\",\"accessToken\":\"access-token\"}}";

        assertThat(interceptor.sanitizeBody("/api/v1/auth/login", request)).isEqualTo(R);
        assertThat(interceptor.sanitizeBody("/api/v1/auth/login/", response)).isEqualTo(R);
    }

    @DisplayName("일반 경로는 비밀번호·토큰 값만 가리고 나머지는 남긴다")
    @Test
    void secretFieldsAreMasked() {
        String body = "{\"email\":\"admin@quitmate.co.kr\",\"password\":\"secret-password\","
                + "\"data\":{\"accessToken\":\"access-token\",\"refreshToken\":\"refresh-token\",\"authCode\":\"123456\"},\"ok\":true}";

        String sanitized = interceptor.sanitizeBody("/api/v1/users", body);

        assertThat(sanitized).contains("\"email\":\"admin@quitmate.co.kr\"", "\"ok\":true");
        assertThat(sanitized).contains("\"password\":\"" + R + "\"", "\"accessToken\":\"" + R + "\"",
                "\"refreshToken\":\"" + R + "\"", "\"authCode\":\"" + R + "\"");
        assertThat(sanitized).doesNotContain("secret-password", "access-token", "refresh-token", "123456");
    }

    @DisplayName("미션 이력 상세 응답의 좌표 목록(locationData.addresses)을 가리고 인증 횟수는 남긴다")
    @Test
    void missionHistoryAddressesAreMasked() {
        String body = "{\"statusCode\":200,\"data\":{\"missionHistoryId\":7,\"category\":\"LOCATION\","
                + "\"locationData\":{\"gpsVerifyCount\":3,\"addresses\":[\"37.5665,126.9780\",\"37.5651,126.9895\"]},"
                + "\"photoData\":null}}";

        String sanitized = interceptor.sanitizeBody("/api/v1/admin/mission-history/7", body);

        assertThat(sanitized).contains("\"gpsVerifyCount\":3", "\"category\":\"LOCATION\"", "\"addresses\":\"" + R + "\"");
        assertThat(sanitized).doesNotContain("37.5665", "126.9780", "37.5651", "126.9895");
    }

    @DisplayName("배열 안 객체의 주소·위도·경도·정확도도 가린다")
    @Test
    void locationFieldsInsideArraysAreMasked() {
        String body = "{\"data\":{\"content\":["
                + "{\"id\":1,\"address\":\"서울시중구세종대로110\",\"latitude\":37.5665,\"longitude\":126.978,\"locationAccuracy\":12},"
                + "{\"id\":2,\"lastSmokeAddress\":\"부산시\",\"nested\":[{\"latitude\":35.1796}]}"
                + "]}}";

        String sanitized = interceptor.sanitizeBody("/api/v1/users", body);

        assertThat(sanitized).contains("\"id\":1", "\"id\":2");
        assertThat(sanitized).contains("\"address\":\"" + R + "\"", "\"latitude\":\"" + R + "\"",
                "\"longitude\":\"" + R + "\"", "\"locationAccuracy\":\"" + R + "\"", "\"lastSmokeAddress\":\"" + R + "\"");
        assertThat(sanitized).doesNotContain("세종대로", "37.5665", "126.978", "부산시", "35.1796");
    }

    @DisplayName("키 이름은 대소문자·밑줄·하이픈과 관계없이 가린다")
    @Test
    void keyMatchingIgnoresCaseAndSeparators() {
        String body = "{\"PASSWORD\":\"p1\",\"Access_Token\":\"t1\",\"ADDRESS\":\"a1\",\"Latitude\":1.5,"
                + "\"last_smoke_address\":\"a2\",\"location-accuracy\":9,\"nickName\":\"닉네임\"}";

        String sanitized = interceptor.sanitizeBody("/api/v1/users", body);

        assertThat(sanitized).contains("\"nickName\":\"닉네임\"");
        assertThat(sanitized).doesNotContain("\"p1\"", "\"t1\"", "\"a1\"", "1.5", "\"a2\"", ":9");
    }

    @DisplayName("값이 null이면 보냈는지만 보이도록 null로 남긴다")
    @Test
    void nullValueStaysNull() {
        String sanitized = interceptor.sanitizeBody("/api/v1/users", "{\"latitude\":null,\"address\":\"서울\"}");

        assertThat(sanitized).contains("\"latitude\":null", "\"address\":\"" + R + "\"");
    }

    @DisplayName("민감 키가 없는 JSON 본문은 그대로 남긴다")
    @Test
    void bodyWithoutSensitiveFieldIsKept() {
        String body = "{\"statusCode\":200,\"data\":{\"content\":[{\"id\":1,\"nickName\":\"닉네임\"}]}}";

        assertThat(interceptor.sanitizeBody("/api/v1/users", body)).isEqualTo(body);
        assertThat(interceptor.sanitizeBody("/api/v1/users", "\"text\"")).isEqualTo("\"text\"");
    }

    @DisplayName("JSON으로 읽지 못한 본문은 어디에 무엇이 있는지 몰라 통째로 가린다")
    @Test
    void unparsableBodyIsRedacted() {
        assertThat(interceptor.sanitizeBody("/api/v1/users", "address=서울&latitude=37.5")).isEqualTo(R);
        assertThat(interceptor.sanitizeBody("/api/v1/users", "{\"address\":\"서울\"")).isEqualTo(R);
    }

    @DisplayName("본문이 없으면 빈 문자열")
    @Test
    void emptyBody() {
        assertThat(interceptor.sanitizeBody("/api/v1/users", "")).isEmpty();
        assertThat(interceptor.sanitizeBody("/api/v1/users", null)).isEmpty();
    }

    @DisplayName("요청 로그에 미션 이력 좌표 원문이 남지 않는다")
    @Test
    void missionHistoryLogHasNoCoordinates() throws Exception {
        MockHttpServletRequest rawRequest = new MockHttpServletRequest("GET", "/api/v1/admin/mission-history/7");
        ContentCachingRequestWrapper request = new ContentCachingRequestWrapper(rawRequest);
        ContentCachingResponseWrapper response = jsonResponse("{\"data\":{\"locationData\":{\"gpsVerifyCount\":1,"
                + "\"addresses\":[\"37.5665,126.9780\"]}}}");

        interceptor.afterCompletion(request, response, dummyHandler(), null);

        String logMessage = appender.list.get(0).getFormattedMessage();
        assertThat(logMessage).contains("Request Method: GET", "Response Body:", "\"addresses\":\"" + R + "\"");
        assertThat(logMessage).doesNotContain("37.5665", "126.9780");
    }

    @DisplayName("로그인 요청 로그에 비밀번호·토큰 원문이 남지 않는다")
    @Test
    void loginLogHasNoSecrets() throws Exception {
        MockHttpServletRequest rawRequest = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        rawRequest.setContentType("application/json");
        rawRequest.setContent("{\"email\":\"admin@quitmate.co.kr\",\"password\":\"secret-password\"}"
                .getBytes(StandardCharsets.UTF_8));
        ContentCachingRequestWrapper request = new ContentCachingRequestWrapper(rawRequest);
        request.getInputStream().readAllBytes();
        ContentCachingResponseWrapper response = jsonResponse("{\"data\":{\"accessToken\":\"access-token\"}}");

        interceptor.afterCompletion(request, response, dummyHandler(), null);

        String logMessage = appender.list.get(0).getFormattedMessage();
        assertThat(logMessage).contains("Request Body: " + R, "Response Body: " + R);
        assertThat(logMessage).doesNotContain("secret-password", "access-token");
    }

    @DisplayName("JSON이 아닌 Content-Type 본문은 지금처럼 로그에 남기지 않는다")
    @Test
    void nonJsonContentTypeIsNotLogged() throws Exception {
        MockHttpServletRequest rawRequest = new MockHttpServletRequest("POST", "/api/v1/storage/NOTICE");
        rawRequest.setContentType("text/plain");
        rawRequest.setContent("address=서울".getBytes(StandardCharsets.UTF_8));
        ContentCachingRequestWrapper request = new ContentCachingRequestWrapper(rawRequest);
        request.getInputStream().readAllBytes();
        ContentCachingResponseWrapper response = new ContentCachingResponseWrapper(new MockHttpServletResponse());
        response.setContentType("text/plain");
        response.getWriter().write("ok");
        response.getWriter().flush();

        interceptor.afterCompletion(request, response, dummyHandler(), null);

        String logMessage = appender.list.get(0).getFormattedMessage();
        assertThat(logMessage).doesNotContain("Request Body:", "Response Body:", "서울");
    }

    private ContentCachingResponseWrapper jsonResponse(String body) throws Exception {
        ContentCachingResponseWrapper response = new ContentCachingResponseWrapper(new MockHttpServletResponse());
        response.setContentType("application/json");
        response.getWriter().write(body);
        response.getWriter().flush();
        return response;
    }

    private HandlerMethod dummyHandler() throws NoSuchMethodException {
        return new HandlerMethod(new DummyController(), DummyController.class.getDeclaredMethod("handle"));
    }

    private static class DummyController {
        public void handle() {
        }
    }
}
