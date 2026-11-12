package com.quitmate.user.userPass.service;

import com.quitmate.global.exception.ConflictException;
import com.quitmate.global.exception.NotFoundException;
import com.quitmate.global.exception.QuitmateException;
import com.quitmate.global.page.response.PageCustom;
import com.quitmate.global.security.AdminAccessService;
import com.quitmate.user.userPass.entity.UserPass;
import com.quitmate.user.userPass.repository.UserPassRepository;
import com.quitmate.user.userPass.service.request.UserPassGrantServiceRequest;
import com.quitmate.user.userPass.service.request.UserPassListServiceRequest;
import com.quitmate.user.userPass.service.request.UserPassRevokeServiceRequest;
import com.quitmate.user.userPass.service.response.UserPassResponse;
import com.quitmate.user.users.entity.User;
import com.quitmate.user.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * QuitMate 90 이용권 목록·부여·회수(KAN-147). 관리자(role = ADMIN)만 쓸 수 있다.
 *
 * <p>시각은 앱 서버와 같이 한국 시간(LocalDateTime)으로 저장·비교한다. 기간은 [startsAt, endsAt)이다.
 * 테이블에 처리자 컬럼이 없어, 누가 부여·회수했는지는 서버 로그(관리자 ID)로만 남는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPassService {

    static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final String UNKNOWN_USER = "해당 회원은 존재하지 않습니다.";
    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AdminAccessService adminAccessService;
    private final UserPassRepository userPassRepository;
    private final UserRepository userRepository;

    public PageCustom<UserPassResponse> getUserPasses(UserPassListServiceRequest request) {
        adminAccessService.getCurrentAdminId();
        LocalDateTime now = now();
        PageRequest pageable = PageRequest.of(request.getPage() - 1, request.getSize());
        return PageCustom.of(userPassRepository
                .findUserPassList(request.getUserId(), request.getStatus(), now, pageable)
                .map(dto -> UserPassResponse.of(dto, now)));
    }

    @Transactional
    public UserPassResponse grant(UserPassGrantServiceRequest request) {
        Long adminId = adminAccessService.getCurrentAdminId();
        LocalDateTime now = now();
        if (!request.getEndsAt().isAfter(request.getStartsAt())) {
            throw new QuitmateException("만료 시각은 시작 시각보다 뒤여야 합니다.");
        }
        if (!request.getEndsAt().isAfter(now)) {
            throw new QuitmateException("만료 시각이 이미 지났습니다. 지금 이후로 지정해 주세요.");
        }

        // 같은 회원에게 동시에 부여해도 겹침 확인이 엇갈리지 않도록 회원 행을 잠근 뒤 확인한다.
        userRepository.lockActiveUserId(request.getUserId())
                .orElseThrow(() -> new NotFoundException(UNKNOWN_USER));
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException(UNKNOWN_USER));

        List<UserPass> overlapping = userPassRepository.findOverlapping(user.getId(), request.getProduct(),
                request.getStartsAt(), request.getEndsAt());
        if (!overlapping.isEmpty()) {
            UserPass existing = overlapping.get(0);
            throw new ConflictException("같은 기간에 유효한 이용권이 이미 있습니다. (이용권 ID " + existing.getId() + ", "
                    + existing.getStartsAt().format(PERIOD_FORMAT) + " ~ "
                    + existing.getEndsAt().format(PERIOD_FORMAT) + ")");
        }

        UserPass saved = userPassRepository.save(UserPass.adminGrant(user.getId(), request.getProduct(),
                request.getStartsAt(), request.getEndsAt(), request.getMemo()));
        log.info("이용권 부여 - 관리자 {} → 회원 {}, 이용권 {} ({} ~ {})", adminId, user.getId(), saved.getId(),
                saved.getStartsAt(), saved.getEndsAt());
        return UserPassResponse.of(saved, user.getEmail(), user.getNickName(), now);
    }

    @Transactional
    public UserPassResponse revoke(UserPassRevokeServiceRequest request) {
        Long adminId = adminAccessService.getCurrentAdminId();
        LocalDateTime now = now();
        UserPass pass = userPassRepository.findByIdForUpdate(request.getPassId())
                .orElseThrow(() -> new NotFoundException("해당 이용권이 없습니다."));
        if (pass.isRevoked()) {
            throw new ConflictException("이미 회수된 이용권입니다.");
        }
        if (pass.isEndedAt(now)) {
            throw new ConflictException("이미 만료된 이용권이라 회수할 수 없습니다.");
        }
        String memo = pass.memoWithRevokeReason(request.getReason());
        if (memo != null && memo.length() > UserPass.MEMO_MAX_LENGTH) {
            throw new QuitmateException("메모가 " + UserPass.MEMO_MAX_LENGTH + "자를 넘습니다. 회수 사유를 줄여 주세요. "
                    + "(부여 메모와 합쳐 " + memo.length() + "자)");
        }

        pass.revoke(now, request.getReason());
        log.info("이용권 회수 - 관리자 {} → 회원 {}, 이용권 {}", adminId, pass.getUserId(), pass.getId());

        User user = userRepository.findById(pass.getUserId()).orElse(null);
        return UserPassResponse.of(pass, user == null ? null : user.getEmail(),
                user == null ? null : user.getNickName(), now);
    }

    LocalDateTime now() {
        return LocalDateTime.now(KOREA);
    }
}
