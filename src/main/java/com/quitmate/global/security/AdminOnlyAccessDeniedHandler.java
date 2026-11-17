package com.quitmate.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quitmate.global.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 로그인은 됐지만 관리자(ADMIN) 권한이 없는 요청을 403 ApiResponse JSON으로 돌려준다(KAN-206).
 * 필터 단계라 ApiControllerAdvice가 동작하지 않으므로 JwtExceptionHandlerFilter처럼 응답을 직접 쓴다.
 * 관리자 웹은 401에서만 로그아웃하므로, 403은 화면에 사유만 보인다.
 */
@Slf4j
public class AdminOnlyAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.warn("관리자가 아닌 계정의 요청을 막음 - {} {} {}", principalName(), request.getMethod(), request.getRequestURI());

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.of(HttpStatus.FORBIDDEN, AdminAccessService.ADMIN_ONLY, null)));
    }

    private String principalName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof com.quitmate.global.jwt.dto.LoginUserInfo tokenUser) {
            return "userId " + tokenUser.getUserId();
        }
        return "인증 없음";
    }
}
