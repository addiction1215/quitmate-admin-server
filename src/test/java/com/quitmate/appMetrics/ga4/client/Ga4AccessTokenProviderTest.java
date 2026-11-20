package com.quitmate.appMetrics.ga4.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import com.quitmate.appMetrics.ga4.config.Ga4Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 서비스 계정 키로 토큰을 받는지, 가짜 토큰 서버(google-http-client MockHttpTransport)로 확인한다. 실제 Google에는 연결하지 않는다.
 */
class Ga4AccessTokenProviderTest {

    private static final String TOKEN_URI = "https://oauth2.example.test/token";
    private static final String TOKEN_RESPONSE =
            "{\"access_token\":\"fake-ga4-token\",\"expires_in\":3600,\"token_type\":\"Bearer\"}";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FakeTokenServer tokenServer = new FakeTokenServer();

    @TempDir
    Path tempDir;

    @DisplayName("서비스 계정 키로 GA4 읽기 전용 권한 토큰을 받고, 만료 전에는 다시 받지 않는다")
    @Test
    void accessToken() throws Exception {
        // given
        Path keyFile = writeServiceAccountKey(tempDir.resolve("ga4-key.json"));
        Ga4AccessTokenProvider provider = provider(keyFile.toString());

        // when
        String first = provider.accessToken();
        String second = provider.accessToken();

        // then
        assertThat(first).isEqualTo("fake-ga4-token");
        assertThat(second).isEqualTo("fake-ga4-token");
        assertThat(tokenServer.requests).hasSize(1);

        MockLowLevelHttpRequest request = tokenServer.requests.get(0);
        assertThat(request.getUrl()).isEqualTo(TOKEN_URI);
        Map<String, String> form = parseForm(request.getContentAsString());
        assertThat(form.get("grant_type")).isEqualTo("urn:ietf:params:oauth:grant-type:jwt-bearer");
        JsonNode claims = jwtPayload(form.get("assertion"));
        assertThat(claims.get("scope").asText()).isEqualTo(Ga4AccessTokenProvider.SCOPE);
        assertThat(claims.get("iss").asText()).isEqualTo("ga4-reader@quitmate-test.iam.gserviceaccount.com");
    }

    @DisplayName("키 파일이 없으면 503 예외이고, 파일을 두면 다음 호출에서 다시 읽는다")
    @Test
    void missingKeyFileIsRetried() throws Exception {
        Path keyFile = tempDir.resolve("later.json");
        Ga4AccessTokenProvider provider = provider(keyFile.toString());

        assertThatThrownBy(provider::accessToken)
                .isInstanceOf(Ga4UnavailableException.class)
                .hasMessageContaining("NoSuchFileException");

        writeServiceAccountKey(keyFile);
        assertThat(provider.accessToken()).isEqualTo("fake-ga4-token");
    }

    @DisplayName("서비스 계정 형식이 아니거나 깨진 키 파일, 빈 경로는 503 예외")
    @Test
    void invalidKeyFile() throws Exception {
        Path userKey = Files.writeString(tempDir.resolve("user.json"),
                "{\"type\":\"authorized_user\",\"client_id\":\"x\",\"client_secret\":\"y\",\"refresh_token\":\"z\"}");
        Path broken = Files.writeString(tempDir.resolve("broken.json"), "{\"type\":");

        assertThatThrownBy(() -> provider(userKey.toString()).accessToken()).isInstanceOf(Ga4UnavailableException.class);
        assertThatThrownBy(() -> provider(broken.toString()).accessToken()).isInstanceOf(Ga4UnavailableException.class);
        assertThatThrownBy(() -> provider(" ").accessToken())
                .isInstanceOf(Ga4UnavailableException.class)
                .hasMessageContaining("경로");
        assertThat(tokenServer.requests).isEmpty();
    }

    @DisplayName("토큰 서버가 거절하면 503 예외. 사유에 토큰 서버 응답 본문은 넣지 않는다")
    @Test
    void tokenServerRejects() throws Exception {
        tokenServer.status = 400;
        tokenServer.body = "{\"error\":\"invalid_grant\",\"error_description\":\"Invalid JWT Signature.\"}";
        Ga4AccessTokenProvider provider = provider(writeServiceAccountKey(tempDir.resolve("key.json")).toString());

        assertThatThrownBy(provider::accessToken)
                .isInstanceOf(Ga4UnavailableException.class)
                .hasMessageStartingWith("GA4 토큰을 받지 못했습니다(")
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain("invalid_grant", "Signature"));
    }

    private Ga4AccessTokenProvider provider(String credentialsPath) {
        return new Ga4AccessTokenProvider(new Ga4Properties("123456789", credentialsPath), () -> tokenServer);
    }

    /** 테스트마다 새로 만든 RSA 키로 서비스 계정 키 파일을 쓴다. */
    private Path writeServiceAccountKey(Path path) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
        Map<String, String> key = new LinkedHashMap<>();
        key.put("type", "service_account");
        key.put("project_id", "quitmate-test");
        key.put("private_key_id", "test-key-id");
        key.put("private_key", pem);
        key.put("client_email", "ga4-reader@quitmate-test.iam.gserviceaccount.com");
        key.put("client_id", "100000000000000000000");
        key.put("token_uri", TOKEN_URI);
        Files.writeString(path, objectMapper.writeValueAsString(key));
        return path;
    }

    private static Map<String, String> parseForm(String body) {
        Map<String, String> form = new LinkedHashMap<>();
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            form.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "");
        }
        return form;
    }

    private JsonNode jwtPayload(String jwt) throws IOException {
        String payload = jwt.split("\\.")[1];
        return objectMapper.readTree(Base64.getUrlDecoder().decode(payload));
    }

    /** 받은 요청을 기록하고 정해 둔 응답을 주는 가짜 토큰 서버 */
    private static class FakeTokenServer extends MockHttpTransport {

        private final List<MockLowLevelHttpRequest> requests = new ArrayList<>();
        private volatile int status = 200;
        private volatile String body = TOKEN_RESPONSE;

        @Override
        public LowLevelHttpRequest buildRequest(String method, String url) {
            MockLowLevelHttpRequest request = new MockLowLevelHttpRequest(url) {
                @Override
                public LowLevelHttpResponse execute() throws IOException {
                    return new MockLowLevelHttpResponse()
                            .setStatusCode(status)
                            .setContentType("application/json; charset=UTF-8")
                            .setContent(body);
                }
            };
            requests.add(request);
            return request;
        }
    }
}
