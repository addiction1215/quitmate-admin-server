package com.quitmate.global.appserver;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppServerTokenIssuerTest {

    /** 테스트 전용 키(운영 키 아님). 64바이트를 Base64로 적은 값 */
    static final String BASE64_TEST_KEY =
            "TD3ANZkMruvvCLpkQ/z++uiK7VMPV6eJMDj8fc7a9RawW03Z5vNQkg3Ko6fuTIiQ/cisBie3Hly56bzoPu+Ezw==";
    /** Base64로 풀리지 않는 글자(-, !)가 든 테스트 전용 키. 앱 서버는 이런 키를 UTF-8 바이트로 쓴다. */
    private static final String RAW_TEST_KEY = "quitmate-app-server-raw-secret-for-test-only!!";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DisplayName("앱 서버 필터가 받는 모양으로 만든다: tokenType=ACCESS, subject={userId, role=ADMIN}, jti, 5분 만료")
    @Test
    void issueAppServerAccessToken() throws Exception {
        // given
        Instant now = Instant.parse("2026-11-12T01:00:00Z");
        AppServerTokenIssuer issuer = new AppServerTokenIssuer(properties(BASE64_TEST_KEY), objectMapper,
                Clock.fixed(now, ZoneOffset.UTC));

        // when
        String token = issuer.issue(7L);

        // then
        Jws<Claims> jws = parse(token, Keys.hmacShaKeyFor(Decoders.BASE64.decode(BASE64_TEST_KEY)), now);
        Claims claims = jws.getBody();
        assertThat(claims.get("tokenType", String.class)).isEqualTo("ACCESS");
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getIssuedAt()).isEqualTo(Date.from(now));
        assertThat(claims.getExpiration()).isEqualTo(Date.from(now.plus(Duration.ofMinutes(5))));
        // 64바이트 키라 앱 서버 signWith(key)와 같이 HS512로 서명된다.
        assertThat(jws.getHeader().getAlgorithm()).isEqualTo("HS512");

        JsonNode subject = objectMapper.readTree(claims.getSubject());
        assertThat(subject.size()).isEqualTo(2);
        assertThat(subject.get("userId").asLong()).isEqualTo(7L);
        assertThat(subject.get("role").asText()).isEqualTo("ADMIN");
    }

    @DisplayName("앱 서버 JwtTokenProvider가 읽는 순서(ACCESS 확인 → 만료 확인 → subject를 LoginUserInfo로 읽기)를 통과한다")
    @Test
    void appServerCanReadToken() throws Exception {
        // given
        AppServerTokenIssuer issuer = new AppServerTokenIssuer(properties(BASE64_TEST_KEY), objectMapper);
        String token = issuer.issue(3L);
        Key appServerKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(BASE64_TEST_KEY));

        // when: 앱 서버 isAccessToken + getAuthentication과 같은 판단
        Claims claims = Jwts.parserBuilder().setSigningKey(appServerKey).build().parseClaimsJws(token).getBody();
        boolean accessToken = "ACCESS".equals(claims.get("tokenType", String.class))
                && !claims.getExpiration().before(new Date());
        AppLoginUserInfo loginUserInfo = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                .readValue(claims.getSubject(), AppLoginUserInfo.class);

        // then
        assertThat(accessToken).isTrue();
        assertThat(loginUserInfo.userId).isEqualTo(3L);
        assertThat(loginUserInfo.role).isEqualTo(AppRole.ADMIN);
    }

    @DisplayName("Base64로 풀리지 않는 키는 앱 서버처럼 UTF-8 바이트로 서명한다")
    @Test
    void rawKey() {
        // given
        AppServerTokenIssuer issuer = new AppServerTokenIssuer(properties(RAW_TEST_KEY), objectMapper);

        // when
        String token = issuer.issue(1L);

        // then
        Key rawKey = Keys.hmacShaKeyFor(RAW_TEST_KEY.getBytes(StandardCharsets.UTF_8));
        assertThat(parse(token, rawKey, Instant.now()).getBody().get("tokenType", String.class)).isEqualTo("ACCESS");
    }

    @DisplayName("다른 키로는 검증되지 않는다")
    @Test
    void otherKeyFails() {
        // given
        String token = new AppServerTokenIssuer(properties(BASE64_TEST_KEY), objectMapper).issue(1L);
        Key otherKey = Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS512);

        // when // then
        assertThatThrownBy(() -> parse(token, otherKey, Instant.now()))
                .isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
    }

    @DisplayName("서명 키가 없으면(빈 값, 빌드 때 env가 없어 들어간 null) 503으로 알린다")
    @Test
    void missingKey() {
        for (String missing : new String[]{null, "", "  ", "null"}) {
            AppServerTokenIssuer issuer = new AppServerTokenIssuer(properties(missing), objectMapper);
            assertThatThrownBy(() -> issuer.issue(1L))
                    .isInstanceOf(AppServerException.class)
                    .satisfies(e -> assertThat(((AppServerException) e).getStatus())
                            .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        }
    }

    @DisplayName("너무 짧은 키는 503으로 알린다")
    @Test
    void weakKey() {
        AppServerTokenIssuer issuer = new AppServerTokenIssuer(properties("short-key!"), objectMapper);

        assertThatThrownBy(() -> issuer.issue(1L))
                .isInstanceOf(AppServerException.class)
                .hasMessageContaining("너무 짧습니다");
    }

    static AppServerProperties properties(String key) {
        return new AppServerProperties("http://localhost:0", key, Duration.ofMinutes(5), Duration.ofSeconds(1),
                Duration.ofSeconds(2));
    }

    private static Jws<Claims> parse(String token, Key key, Instant now) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .setClock(() -> Date.from(now))
                .build()
                .parseClaimsJws(token);
    }

    /** 앱 서버 com.addiction.jwt.dto.LoginUserInfo와 같은 모양 */
    static class AppLoginUserInfo {
        public Long userId;
        public AppRole role;
    }

    /** 앱 서버 Role */
    enum AppRole {
        USER, ADMIN
    }
}
