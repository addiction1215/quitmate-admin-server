package com.quitmate.global.jwt.filter;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import com.quitmate.global.jwt.JwtTokenProvider;
import com.quitmate.global.jwt.exception.JwtTokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

/*
 * Token인증이 필요한 API 메소드 호출이전 Header에 저장되어있는 토큰을 가져와 유효여부를 체크한다.
 * */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    private static final String[] excludePath = {
            "/api/v1/auth",                 //로그인 예정
            "/api/v1/confirmEmail",
            "/docs",                        //API문서는 예외
            "/health-check"
    };

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return Arrays.stream(excludePath).anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Header에서 토큰 받아옴
            String token = jwtTokenProvider.resolveToken(request);
            // 유효한 토큰인지 확인
            if (token != null && jwtTokenProvider.extractSubject(token)) {
                // 토큰이 유효하면 토큰으로부터 유저 정보를 세팅
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                throw new JwtTokenException("JWT토큰이 수신되지 않았거나 형식이 맞지않습니다.");
            }
        } catch (ExpiredJwtException e) {
            throw new JwtTokenException("JWT토큰이 만료되었습니다.", e);
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtTokenException("JWT토큰이 수신되지 않았거나 형식이 맞지않습니다.", e);
        }

        filterChain.doFilter(request, response);
    }
}
