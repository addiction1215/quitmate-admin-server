package com.quitmate.user.userPass.service;

import com.quitmate.IntegrationTestSupport;
import com.quitmate.global.exception.ConflictException;
import com.quitmate.global.exception.ForbiddenException;
import com.quitmate.global.exception.NotFoundException;
import com.quitmate.global.exception.QuitmateException;
import com.quitmate.global.page.response.PageCustom;
import com.quitmate.user.userPass.entity.UserPass;
import com.quitmate.user.userPass.enums.UserPassProduct;
import com.quitmate.user.userPass.enums.UserPassSource;
import com.quitmate.user.userPass.enums.UserPassStatus;
import com.quitmate.user.userPass.repository.UserPassJpaRepository;
import com.quitmate.user.userPass.service.request.UserPassGrantServiceRequest;
import com.quitmate.user.userPass.service.request.UserPassListServiceRequest;
import com.quitmate.user.userPass.service.request.UserPassRevokeServiceRequest;
import com.quitmate.user.userPass.service.response.UserPassResponse;
import com.quitmate.user.users.entity.User;
import com.quitmate.user.users.entity.enums.Role;
import com.quitmate.user.users.entity.enums.SettingStatus;
import com.quitmate.user.users.entity.enums.SnsType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;

class UserPassServiceTest extends IntegrationTestSupport {

    @Autowired
    private UserPassService userPassService;

    @Autowired
    private UserPassJpaRepository userPassJpaRepository;

    private LocalDateTime today;
    private User admin;
    private User member;

    @BeforeEach
    void setUp() {
        today = LocalDateTime.now(UserPassService.KOREA).truncatedTo(ChronoUnit.DAYS);
        admin = userRepository.save(createUser("admin@test.com", "1234", SnsType.NORMAL, SettingStatus.COMPLETE));
        User user = createUser("member@test.com", "1234", SnsType.NORMAL, SettingStatus.COMPLETE);
        user.updateRole(Role.USER);
        member = userRepository.save(user);
        loginAs(admin);
    }

    @AfterEach
    void deletePasses() {
        userPassJpaRepository.deleteAllInBatch();
    }

    @DisplayName("관리자가 기간을 정해 부여하면 출처 ADMIN_GRANT 이용권이 생기고, 오늘 시작이면 바로 이용 중이다")
    @Test
    void grant() {
        // when
        UserPassResponse response = userPassService.grant(grantRequest(member, today, today.plusDays(90), " 베타 테스터 "));

        // then
        assertThat(response.getId()).isNotNull();
        assertThat(response)
                .extracting("userId", "email", "product", "source", "startsAt", "endsAt", "revokedAt", "status", "memo")
                .containsExactly(member.getId(), "member@test.com", UserPassProduct.QUITMATE_90,
                        UserPassSource.ADMIN_GRANT, today, today.plusDays(90), null, UserPassStatus.ACTIVE,
                        "베타 테스터");

        UserPass saved = userPassJpaRepository.findById(response.getId()).orElseThrow();
        assertThat(saved.getSource()).isEqualTo(UserPassSource.ADMIN_GRANT);
        assertThat(saved.getCreatedDate()).isNotNull();
        assertThat(saved.getUpdatedDate()).isNotNull();
    }

    @DisplayName("같은 기간에 유효한 이용권이 있으면 부여를 거부한다(409)")
    @Test
    void grantOverlapping() {
        // given
        UserPass existing = save(pass(member, today.plusDays(10), today.plusDays(100)));

        // when // then: 기존 기간 안쪽, 앞쪽 걸침, 뒤쪽 걸침, 통째로 감싸기 모두 거부
        for (LocalDateTime[] period : new LocalDateTime[][]{
                {today.plusDays(20), today.plusDays(50)},
                {today, today.plusDays(11)},
                {today.plusDays(99), today.plusDays(190)},
                {today, today.plusDays(200)}}) {
            assertThatThrownBy(() -> userPassService.grant(grantRequest(member, period[0], period[1], null)))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("같은 기간에 유효한 이용권이 이미 있습니다")
                    .hasMessageContaining("이용권 ID " + existing.getId());
        }
        assertThat(userPassJpaRepository.count()).isEqualTo(1);
    }

    @DisplayName("앞 이용권의 만료 시각에 시작하는 연장 부여, 회수된 이용권과 겹치는 부여, 다른 회원 부여는 된다")
    @Test
    void grantNotOverlapping() {
        // given
        save(pass(member, today.minusDays(10), today.plusDays(80)));
        save(revokedPass(member, today.plusDays(200), today.plusDays(290), today.minusDays(1)));
        User other = userRepository.save(createUser("other@test.com", "1234", SnsType.NORMAL, SettingStatus.COMPLETE));

        // when
        UserPassResponse extended = userPassService.grant(grantRequest(member, today.plusDays(80), today.plusDays(170), null));
        UserPassResponse overRevoked = userPassService.grant(grantRequest(member, today.plusDays(200), today.plusDays(290), null));
        UserPassResponse otherUser = userPassService.grant(grantRequest(other, today, today.plusDays(90), null));

        // then
        assertThat(extended.getStatus()).isEqualTo(UserPassStatus.SCHEDULED);
        assertThat(overRevoked.getStatus()).isEqualTo(UserPassStatus.SCHEDULED);
        assertThat(otherUser.getStatus()).isEqualTo(UserPassStatus.ACTIVE);
        assertThat(userPassJpaRepository.count()).isEqualTo(5);
    }

    @DisplayName("만료 시각이 시작 시각 이전이거나 이미 지났으면 400")
    @Test
    void grantInvalidPeriod() {
        assertThatThrownBy(() -> userPassService.grant(grantRequest(member, today.plusDays(5), today.plusDays(5), null)))
                .isInstanceOf(QuitmateException.class)
                .hasMessage("만료 시각은 시작 시각보다 뒤여야 합니다.");
        assertThatThrownBy(() -> userPassService.grant(grantRequest(member, today.minusDays(90), today.minusDays(1), null)))
                .isInstanceOf(QuitmateException.class)
                .hasMessage("만료 시각이 이미 지났습니다. 지금 이후로 지정해 주세요.");
        assertThat(userPassJpaRepository.count()).isZero();
    }

    @DisplayName("없는 회원에게는 부여할 수 없다(404)")
    @Test
    void grantUnknownUser() {
        assertThatThrownBy(() -> userPassService.grant(UserPassGrantServiceRequest.builder()
                .userId(-1L).startsAt(today).endsAt(today.plusDays(90)).build()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("해당 회원은 존재하지 않습니다.");
    }

    @DisplayName("관리자가 아니면 목록·부여·회수 모두 403")
    @Test
    void notAdmin() {
        // given
        UserPass pass = save(pass(member, today, today.plusDays(90)));
        loginAs(member);

        // when // then
        assertThatThrownBy(() -> userPassService.getUserPasses(UserPassListServiceRequest.builder().page(1).size(10).build()))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> userPassService.grant(grantRequest(member, today.plusDays(90), today.plusDays(180), null)))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> userPassService.revoke(revokeRequest(pass, null)))
                .isInstanceOf(ForbiddenException.class);
        assertThat(userPassJpaRepository.findById(pass.getId()).orElseThrow().getRevokedAt()).isNull();
    }

    @DisplayName("회수하면 revoked_at이 찍히고 회수 사유가 메모 뒤에 붙으며, 같은 기간에 다시 부여할 수 있다")
    @Test
    void revoke() {
        // given
        UserPass pass = save(passWithMemo(member, today, today.plusDays(90), "베타 테스터"));

        // when
        UserPassResponse response = userPassService.revoke(revokeRequest(pass, " 테스트 종료 "));

        // then
        assertThat(response.getStatus()).isEqualTo(UserPassStatus.REVOKED);
        assertThat(response.getRevokedAt()).isNotNull();
        assertThat(response.getMemo()).isEqualTo("베타 테스터\n[회수] 테스트 종료");
        UserPass revoked = userPassJpaRepository.findById(pass.getId()).orElseThrow();
        assertThat(revoked.getRevokedAt()).isNotNull();
        assertThat(revoked.getMemo()).isEqualTo("베타 테스터\n[회수] 테스트 종료");

        UserPassResponse regranted = userPassService.grant(grantRequest(member, today, today.plusDays(90), null));
        assertThat(regranted.getStatus()).isEqualTo(UserPassStatus.ACTIVE);
    }

    @DisplayName("사유 없이 회수하면 메모는 그대로다")
    @Test
    void revokeWithoutReason() {
        UserPass pass = save(pass(member, today.plusDays(1), today.plusDays(91)));

        UserPassResponse response = userPassService.revoke(revokeRequest(pass, "  "));

        assertThat(response.getStatus()).isEqualTo(UserPassStatus.REVOKED);
        assertThat(response.getMemo()).isNull();
    }

    @DisplayName("이미 회수했거나 만료된 이용권은 회수할 수 없다(409), 없는 이용권은 404")
    @Test
    void revokeNotAllowed() {
        UserPass revoked = save(pass(member, today, today.plusDays(90)));
        userPassService.revoke(revokeRequest(revoked, null));
        UserPass expired = save(pass(member, today.minusDays(100), today.minusDays(10)));

        assertThatThrownBy(() -> userPassService.revoke(revokeRequest(revoked, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("이미 회수된 이용권입니다.");
        assertThatThrownBy(() -> userPassService.revoke(revokeRequest(expired, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("이미 만료된 이용권이라 회수할 수 없습니다.");
        assertThatThrownBy(() -> userPassService.revoke(UserPassRevokeServiceRequest.builder().passId(-1L).build()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("해당 이용권이 없습니다.");
    }

    @DisplayName("부여 메모와 회수 사유를 합쳐 255자를 넘으면 400이고 회수되지 않는다")
    @Test
    void revokeMemoTooLong() {
        UserPass pass = save(passWithMemo(member, today, today.plusDays(90), "가".repeat(200)));

        assertThatThrownBy(() -> userPassService.revoke(revokeRequest(pass, "나".repeat(60))))
                .isInstanceOf(QuitmateException.class)
                .hasMessageContaining("회수 사유를 줄여 주세요");
        assertThat(userPassJpaRepository.findById(pass.getId()).orElseThrow().getRevokedAt()).isNull();
    }

    @DisplayName("목록은 최근 부여 순이고, 상태·회원으로 거를 수 있으며 회원 이메일·닉네임이 함께 나온다")
    @Test
    void getUserPasses() {
        // given
        UserPass expired = save(pass(member, today.minusDays(100), today.minusDays(10)));
        UserPass active = save(pass(member, today.minusDays(10), today.plusDays(80)));
        UserPass scheduled = save(pass(member, today.plusDays(80), today.plusDays(170)));
        UserPass revoked = save(revokedPass(admin, today, today.plusDays(90), today));

        // when
        PageCustom<UserPassResponse> all = userPassService.getUserPasses(list(null, null));
        PageCustom<UserPassResponse> onlyActive = userPassService.getUserPasses(list(null, UserPassStatus.ACTIVE));
        PageCustom<UserPassResponse> onlyScheduled = userPassService.getUserPasses(list(null, UserPassStatus.SCHEDULED));
        PageCustom<UserPassResponse> onlyExpired = userPassService.getUserPasses(list(null, UserPassStatus.EXPIRED));
        PageCustom<UserPassResponse> onlyRevoked = userPassService.getUserPasses(list(null, UserPassStatus.REVOKED));
        PageCustom<UserPassResponse> ofMember = userPassService.getUserPasses(list(member.getId(), null));

        // then
        assertThat(all.getContent())
                .extracting("id", "status", "email", "nickName")
                .containsExactly(
                        tuple(revoked.getId(), UserPassStatus.REVOKED, "admin@test.com", "테스트 닉네임"),
                        tuple(scheduled.getId(), UserPassStatus.SCHEDULED, "member@test.com", "테스트 닉네임"),
                        tuple(active.getId(), UserPassStatus.ACTIVE, "member@test.com", "테스트 닉네임"),
                        tuple(expired.getId(), UserPassStatus.EXPIRED, "member@test.com", "테스트 닉네임"));
        assertThat(all.getPageInfo().getTotalElement()).isEqualTo(4);
        assertThat(onlyActive.getContent()).extracting("id").containsExactly(active.getId());
        assertThat(onlyScheduled.getContent()).extracting("id").containsExactly(scheduled.getId());
        assertThat(onlyExpired.getContent()).extracting("id").containsExactly(expired.getId());
        assertThat(onlyRevoked.getContent()).extracting("id").containsExactly(revoked.getId());
        assertThat(onlyRevoked.getPageInfo().getTotalElement()).isEqualTo(1);
        assertThat(ofMember.getContent()).extracting("id")
                .containsExactly(scheduled.getId(), active.getId(), expired.getId());
    }

    private void loginAs(User user) {
        given(securityService.getCurrentLoginUserInfo()).willReturn(createLoginUserInfo(user.getId()));
    }

    private UserPass save(UserPass pass) {
        return userPassJpaRepository.save(pass);
    }

    private static UserPass pass(User user, LocalDateTime startsAt, LocalDateTime endsAt) {
        return passOf(user, startsAt, endsAt, null, null);
    }

    private static UserPass revokedPass(User user, LocalDateTime startsAt, LocalDateTime endsAt, LocalDateTime revokedAt) {
        return passOf(user, startsAt, endsAt, null, revokedAt);
    }

    private static UserPass passWithMemo(User user, LocalDateTime startsAt, LocalDateTime endsAt, String memo) {
        return passOf(user, startsAt, endsAt, memo, null);
    }

    private static UserPass passOf(User user, LocalDateTime startsAt, LocalDateTime endsAt, String memo,
                                   LocalDateTime revokedAt) {
        return UserPass.of(user.getId(), UserPassProduct.QUITMATE_90, UserPassSource.ADMIN_GRANT, startsAt, endsAt,
                revokedAt, memo);
    }

    private static UserPassGrantServiceRequest grantRequest(User user, LocalDateTime startsAt, LocalDateTime endsAt,
                                                            String memo) {
        return UserPassGrantServiceRequest.builder()
                .userId(user.getId())
                .startsAt(startsAt)
                .endsAt(endsAt)
                .memo(memo)
                .build();
    }

    private static UserPassRevokeServiceRequest revokeRequest(UserPass pass, String reason) {
        return UserPassRevokeServiceRequest.builder().passId(pass.getId()).reason(reason).build();
    }

    private static UserPassListServiceRequest list(Long userId, UserPassStatus status) {
        return UserPassListServiceRequest.builder().page(1).size(20).userId(userId).status(status).build();
    }
}
