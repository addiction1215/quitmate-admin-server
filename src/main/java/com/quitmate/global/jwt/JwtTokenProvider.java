package com.quitmate.global.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quitmate.global.jwt.dto.LoginUserInfo;
import com.quitmate.global.security.UserRoleAuthorityResolver;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.util.Date;

/*
 * JWTToken의 생성, 유효여부, 복호화 관련 클래스
 * */
@Component
public class JwtTokenProvider {

    private final ObjectMapper objectMapper;
    private final UserRoleAuthorityResolver userRoleAuthorityResolver;
    private final Key key;

    //키 생성하여 의존성 주입
    public JwtTokenProvider(@Value("${jwt.secret-key}") String secretKey, ObjectMapper objectMapper,
                            UserRoleAuthorityResolver userRoleAuthorityResolver) {
        this.objectMapper = objectMapper;
        this.userRoleAuthorityResolver = userRoleAuthorityResolver;
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    // 토큰 생성
    public String generate(String subject, Date expiredAt) {
        return Jwts.builder()
                .setSubject(subject)
                .setExpiration(expiredAt)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    // 토큰 만료여부 체크
    public boolean extractSubject(String accessToken) {
        Claims claims = parseClaims(accessToken);
        return !claims.getExpiration().before(new Date());
    }

    private Claims parseClaims(String accessToken) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(accessToken)
                .getBody();
    }

    // Request의 Header에서 token 값을 가져옵니다. "Authorization" : "TOKEN값'
    public String resolveToken(HttpServletRequest request) {
        String headerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(headerToken) && headerToken.startsWith("Bearer ")) {
            return headerToken.substring(7).trim();
        }
        return null;
    }

    // 토큰에서 회원 정보 추출
    public String getUserPk(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getSubject();
    }

    /**
     * JWT 토큰에서 인증 정보 조회. 권한은 토큰이 아니라 DB 회원의 실제 role이다(KAN-206).
     * 관리자가 아니거나 없는·탈퇴한 계정이면 ADMIN 권한이 없어 SecurityConfig에서 403이 된다.
     */
    public Authentication getAuthentication(String token) throws JsonProcessingException {
        LoginUserInfo loginUserInfo = objectMapper.readValue(this.getUserPk(token), LoginUserInfo.class);
        return new UsernamePasswordAuthenticationToken(loginUserInfo, "",
                userRoleAuthorityResolver.resolve(loginUserInfo.getUserId()));
    }
}
