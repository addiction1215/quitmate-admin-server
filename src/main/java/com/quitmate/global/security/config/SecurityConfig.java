package com.quitmate.global.security.config;

import com.quitmate.global.jwt.JwtTokenProvider;
import com.quitmate.global.jwt.filter.JwtAuthenticationFilter;
import com.quitmate.global.jwt.filter.JwtExceptionHandlerFilter;
import com.quitmate.global.security.AdminOnlyAccessDeniedHandler;
import com.quitmate.user.users.entity.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
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

    private final JwtTokenProvider jwtTokenProvider;

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
                    "https://admin.quitmate.co.kr",
                    "http://localhost:3000",
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

                // 공개 경로를 뺀 모든 API는 관리자만(KAN-206). 권한은 JwtTokenProvider가 DB 회원의 실제 role로 붙인다.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(getPublicMatchers()).permitAll()
                        .anyRequest().hasAuthority(Role.ADMIN.name())
                )
                // 관리자가 아니거나 없는·탈퇴한 계정: 403 + ApiResponse JSON("관리자 계정만 사용할 수 있습니다.")
                .exceptionHandling(handling -> handling
                        .accessDeniedHandler(new AdminOnlyAccessDeniedHandler()))

                .logout((logout) -> logout
                        .logoutSuccessUrl("/"))

                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JwtExceptionHandlerFilter(),
                        JwtAuthenticationFilter.class); //JwtAuthenticationFilter에서 뱉은 에러를 처리하기 위한 Filter

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

