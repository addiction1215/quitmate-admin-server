package com.quitmate;

import com.quitmate.global.security.LoginUserInfo;
import com.quitmate.global.security.SecurityService;
import com.quitmate.user.users.entity.User;
import com.quitmate.user.users.entity.enums.Role;
import com.quitmate.user.users.entity.enums.SettingStatus;
import com.quitmate.user.users.entity.enums.Sex;
import com.quitmate.user.users.entity.enums.SnsType;
import com.quitmate.user.users.repository.UserJpaRepository;
import com.quitmate.user.users.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
@SpringBootTest
public abstract class IntegrationTestSupport {

    @MockitoBean
    protected SecurityService securityService;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected UserJpaRepository userJpaRepository;

    @AfterEach
    public void tearDown() {
        userRepository.deleteAllInBatch();
    }

    protected User createUser(String email, String password, SnsType snsType, SettingStatus settingStatus) {
        return User.builder()
                .email(email)
                .password(bCryptPasswordEncoder.encode(password))
                .nickName("테스트 닉네임")
                .phoneNumber("010-1234-1234")
                .sex(Sex.MALE)
                .role(Role.USER)
                .snsType(snsType)
                .settingStatus(settingStatus)
                .purpose("테스트 목표")
                .build();
    }

    protected LoginUserInfo createLoginUserInfo(Long userId) {
        return LoginUserInfo.builder()
                .userId(userId)
                .build();
    }

}
