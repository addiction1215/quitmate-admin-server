package com.quitmate.user.users.service.impl;

import com.quitmate.global.exception.QuitmateException;
import com.quitmate.user.users.entity.User;
import com.quitmate.user.users.repository.UserRepository;
import com.quitmate.user.users.service.UserReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserReadServiceImpl implements UserReadService {

    private static final String UNKNOWN_USER = "해당 회원은 존재하지 않습니다.";

    private final UserRepository userRepository;

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new QuitmateException(UNKNOWN_USER));
    }


    @Override
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new QuitmateException(UNKNOWN_USER));
    }

}
