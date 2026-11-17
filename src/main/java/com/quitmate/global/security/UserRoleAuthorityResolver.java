package com.quitmate.global.security;

import com.quitmate.user.users.entity.User;
import com.quitmate.user.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 토큰의 userId로 DB 회원을 찾아 실제 role을 권한으로 돌려준다(KAN-206).
 * 관리자 서버 토큰에는 userId만 들어 있어, 토큰만 보고는 관리자인지 알 수 없다.
 * 요청마다 users를 한 번 읽는다. 탈퇴(use_yn = 'N')·없는 계정·role이 없는 계정은 권한 없이 돌려주고,
 * SecurityConfig가 ADMIN 권한만 통과시킨다.
 */
@Component
@RequiredArgsConstructor
public class UserRoleAuthorityResolver {

    private final UserRepository userRepository;

    public List<GrantedAuthority> resolve(Long userId) {
        if (userId == null) {
            return List.of();
        }
        // findById는 use_yn = 'Y'인 회원만 찾는다
        return userRepository.findById(userId)
                .map(User::getRole)
                .<List<GrantedAuthority>>map(role -> List.of(new SimpleGrantedAuthority(role.name())))
                .orElse(List.of());
    }
}
