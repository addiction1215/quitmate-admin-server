package com.quitmate.user.users.service.impl;

import com.quitmate.global.exception.QuitmateException;
import com.quitmate.user.users.entity.User;
import com.quitmate.user.users.service.LoginService;
import com.quitmate.user.users.service.UserReadService;
import com.quitmate.user.users.service.request.LoginServiceRequest;
import com.quitmate.user.users.service.response.LoginResponse;
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

    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final UserReadService userReadService;

    @Override
    public LoginResponse normalLogin(LoginServiceRequest loginServiceRequest) {
        User user = userReadService.findByEmail(loginServiceRequest.getEmail());

        if (!bCryptPasswordEncoder.matches(loginServiceRequest.getPassword(), user.getPassword())) {
            throw new QuitmateException("아이디 또는 패스워드가 일치하지 않습니다.");
        } //3. 비밀번호 체크
        return LoginResponse.of(user);
    }

}
