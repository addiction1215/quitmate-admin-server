package com.quitmate.user.users.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.quitmate.global.exception.QuitmateException;
import com.quitmate.global.jwt.JwtTokenGenerator;
import com.quitmate.global.jwt.dto.JwtToken;
import com.quitmate.global.jwt.dto.LoginUserInfo;
import com.quitmate.user.users.entity.User;
import com.quitmate.user.users.service.LoginService;
import com.quitmate.user.users.service.UserReadService;
import com.quitmate.user.users.service.request.LoginServiceRequest;
import com.quitmate.user.users.service.response.LoginResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    private final JwtTokenGenerator jwtTokenGenerator;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final UserReadService userReadService;
    private final HttpSession httpSession;

    @Override
    public LoginResponse normalLogin(LoginServiceRequest loginServiceRequest) throws JsonProcessingException {
        User user = userReadService.findByEmail(loginServiceRequest.getEmail());

        if (!bCryptPasswordEncoder.matches(loginServiceRequest.getPassword(), user.getPassword())) {
            throw new QuitmateException("아이디 또는 패스워드가 일치하지 않습니다.");
        }

        JwtToken jwtToken = setJwtTokenPushKey(user);
        log.info("로그인 성공 - 사용자: {}, 세션 ID: {}", user.getEmail(), httpSession.getId());

        return LoginResponse.createResponse(user, jwtToken);
    }

    private JwtToken setJwtTokenPushKey(User user) throws JsonProcessingException {
        LoginUserInfo userInfo = LoginUserInfo.of(user.getId());
        return jwtTokenGenerator.generate(userInfo);
    }

}
