package com.quitmate.global.security;

import com.quitmate.global.exception.ForbiddenException;
import com.quitmate.user.users.entity.User;
import com.quitmate.user.users.entity.enums.Role;
import com.quitmate.user.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지금 로그인한 사람이 관리자(role = ADMIN)인지 DB로 확인한다.
 * 관리자 서버 토큰에는 userId만 들어 있어, 토큰을 받은 뒤 권한이 바뀌었거나 다른 토큰이 들어와도 여기서 막는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAccessService {

    private static final String ADMIN_ONLY = "관리자 계정만 사용할 수 있습니다.";

    private final SecurityService securityService;
    private final UserRepository userRepository;

    /**
     * @return 관리자 userId
     * @throws ForbiddenException 관리자가 아니거나 없는 계정이면
     */
    public Long getCurrentAdminId() {
        Long userId = securityService.getCurrentLoginUserInfo().getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ForbiddenException(ADMIN_ONLY));
        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenException(ADMIN_ONLY);
        }
        return userId;
    }
}
