package com.quitmate.user.users.service.impl;

import com.quitmate.global.exception.QuitmateException;
import com.quitmate.global.security.LoginUserInfo;
import com.quitmate.user.users.entity.User;
import com.quitmate.user.users.service.LoginService;
import com.quitmate.user.users.service.UserReadService;
import com.quitmate.user.users.service.request.LoginServiceRequest;
import com.quitmate.user.users.service.response.LoginResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final UserReadService userReadService;
    private final HttpSession httpSession;

    @Override
    public LoginResponse normalLogin(LoginServiceRequest loginServiceRequest) {
        User user = userReadService.findByEmail(loginServiceRequest.getEmail());

        if (!bCryptPasswordEncoder.matches(loginServiceRequest.getPassword(), user.getPassword())) {
            throw new QuitmateException("아이디 또는 패스워드가 일치하지 않습니다.");
        } 

        // 세션에 사용자 정보 저장
        LoginUserInfo loginUserInfo = LoginUserInfo.of(user.getId(), user.getEmail(), user.getRole());
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(loginUserInfo, null, loginUserInfo.getAuthorities());
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // 세션 타임아웃 설정 (1시간 = 3600초)
        httpSession.setMaxInactiveInterval(3600);
        
        log.info("로그인 성공 - 사용자: {}, 세션 ID: {}", user.getEmail(), httpSession.getId());

        return LoginResponse.createResponse(user);
    }

}
