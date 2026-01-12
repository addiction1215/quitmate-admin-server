package com.quitmate.global.security.config;

import com.quitmate.user.users.entity.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    CorsConfigurationSource corsConfigurationSource() {
        return request -> {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedHeaders(Collections.singletonList("*"));
            config.setAllowedMethods(Collections.singletonList("*"));
            config.setAllowedOrigins(Arrays.asList(
                    "https://www.quitmate.co.kr",
                    "https://quitmate.co.kr",
                    "http://localhost:8081",
                    "http://localhost:5173"
            ));
            config.setAllowCredentials(true);
            return config;
        };
    }

    @Bean
    protected SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(corsConfigurer -> corsConfigurer.configurationSource(corsConfigurationSource()))

                .headers((headers) -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))

                // SecurityContext를 세션에 저장하도록 명시적으로 설정
                .securityContext((securityContext) -> securityContext
                        .securityContextRepository(securityContextRepository())
                )

                // Session 설정 - 세션 기반 인증 사용
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // 필요시 세션 생성
                        .maximumSessions(1) // 동시 세션 1개로 제한
                        .maxSessionsPreventsLogin(false) // 새로운 로그인 시 기존 세션 만료
                )

                // 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(getPublicMatchers()).permitAll() // 로그인, 회원가입 등은 인증 없이 접근 가능
                        .anyRequest().authenticated() // 나머지는 인증 필요 (세션이 있어야 함)
                )

                // 로그아웃 설정
                .logout((logout) -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true) // 세션 무효화
                        .deleteCookies("JSESSIONID") // 쿠키 삭제
                        .clearAuthentication(true) // 인증 정보 제거
                        .permitAll()
                )

                // 인증 실패 시 처리 (401 에러 반환)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다.\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"접근 권한이 없습니다.\"}");
                        })
                );

        return http.build();
    }

    private RequestMatcher[] getPublicMatchers() {
        return new RequestMatcher[]{
                // 인증/인가 관련 API (로그인, 회원가입 등)
                new AntPathRequestMatcher("/api/v1/auth/**"),
                // API 문서
                new AntPathRequestMatcher("/docs/**"),
                // 헬스체크
                new AntPathRequestMatcher("/health-check")
        };
    }
}

