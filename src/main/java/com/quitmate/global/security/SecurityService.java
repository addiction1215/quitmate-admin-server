package com.quitmate.global.security;

import com.quitmate.global.exception.UnauthenticatedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {
    public LoginUserInfo getCurrentLoginUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new UnauthenticatedException("로그인이 필요합니다.");
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof LoginUserInfo) {
            return (LoginUserInfo) principal;
        }

        throw new UnauthenticatedException("유효한 인증 정보가 아닙니다.");
    }
}
