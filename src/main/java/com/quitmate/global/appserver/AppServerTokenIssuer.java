package com.quitmate.global.appserver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Clock;
import java.util.Date;
import java.util.UUID;

/**
 * 앱 서버 관리자 API를 부를 때만 쓰는 짧은 토큰을 만든다(KAN-73 댓글 10339에서 정한 방식).
 *
 * <p>앱 서버(addiction) {@code JwtTokenProvider}가 받는 모양과 똑같이 만든다.
 * <ul>
 *   <li>subject: 앱 서버 {@code LoginUserInfo}의 JSON. {@code {"userId":관리자ID,"role":"ADMIN"}}</li>
 *   <li>클레임 {@code tokenType}: {@code ACCESS}. 앱 서버 필터는 ACCESS가 아니면 401을 준다.</li>
 *   <li>{@code jti}(UUID), {@code iat}, {@code exp}(기본 5분)</li>
 *   <li>서명: 앱 서버 jwt.secret-key로 만든 HMAC 키. 키 길이에 맞는 HS 알고리즘을 jjwt가 고른다.</li>
 * </ul>
 * 관리자 서버 자신의 로그인 토큰과는 키도 모양도 다르다. 토큰 값과 서명 키는 로그에 남기지 않는다.
 */
@Component
public class AppServerTokenIssuer {

    static final String TOKEN_TYPE_CLAIM = "tokenType";
    static final String ACCESS_TOKEN_TYPE = "ACCESS";
    static final String ADMIN_ROLE = "ADMIN";

    private static final String KEY_NOT_CONFIGURED =
            "관리자 서버에 앱 서버 서명 키가 설정되지 않아 앱 서버를 부를 수 없습니다.";

    private final AppServerProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private volatile Key signingKey;

    @Autowired
    public AppServerTokenIssuer(AppServerProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, Clock.systemUTC());
    }

    AppServerTokenIssuer(AppServerProperties properties, ObjectMapper objectMapper, Clock clock) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * @param adminId 관리자 userId. 앱 서버는 이 값을 수동 실행자(requested_by)로 남긴다.
     */
    public String issue(Long adminId) {
        if (adminId == null) {
            throw new IllegalArgumentException("관리자 ID가 없습니다.");
        }
        Key key = signingKey();
        long now = clock.millis();
        return Jwts.builder()
                .setSubject(subject(adminId))
                .setIssuedAt(new Date(now))
                .setId(UUID.randomUUID().toString())
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .setExpiration(new Date(now + properties.tokenTtl().toMillis()))
                .signWith(key)
                .compact();
    }

    private String subject(Long adminId) {
        try {
            return objectMapper.writeValueAsString(new AppServerLoginUserInfo(adminId, ADMIN_ROLE));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("앱 서버 토큰 내용을 만들지 못했습니다.", e);
        }
    }

    private Key signingKey() {
        if (!properties.hasJwtSecretKey()) {
            throw new AppServerException(HttpStatus.SERVICE_UNAVAILABLE, KEY_NOT_CONFIGURED);
        }
        Key key = signingKey;
        if (key == null) {
            key = createSigningKey(properties.jwtSecretKey().trim());
            signingKey = key;
        }
        return key;
    }

    /**
     * 앱 서버 JwtTokenProvider.createSigningKey와 같은 규칙: Base64로 풀어 보고, 안 풀리거나 짧으면 UTF-8 바이트를 쓴다.
     */
    static Key createSigningKey(String secretKey) {
        try {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
        } catch (DecodingException | IllegalArgumentException | WeakKeyException e) {
            return createRawSigningKey(secretKey);
        }
    }

    private static Key createRawSigningKey(String secretKey) {
        try {
            return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        } catch (WeakKeyException e) {
            throw new AppServerException(HttpStatus.SERVICE_UNAVAILABLE,
                    "관리자 서버의 앱 서버 서명 키가 너무 짧습니다. 앱 서버 jwt.secret-key와 같은 값인지 확인해 주세요.");
        }
    }

    /**
     * 앱 서버 com.addiction.jwt.dto.LoginUserInfo와 같은 필드(userId, role).
     */
    record AppServerLoginUserInfo(Long userId, String role) {
    }
}
