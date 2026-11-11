package com.quitmate.cigaretteHistoryBatch.service;

import com.quitmate.IntegrationTestSupport;
import com.quitmate.cigaretteHistoryBatch.entity.CigaretteHistoryBatchIssue;
import com.quitmate.cigaretteHistoryBatch.enums.CigaretteHistoryBatchIssueStage;
import com.quitmate.cigaretteHistoryBatch.enums.CigaretteHistoryBatchIssueStatus;
import com.quitmate.cigaretteHistoryBatch.repository.CigaretteHistoryBatchIssueJpaRepository;
import com.quitmate.cigaretteHistoryBatch.service.request.CigaretteHistoryBatchIssueIgnoreServiceRequest;
import com.quitmate.cigaretteHistoryBatch.service.request.CigaretteHistoryBatchIssueListServiceRequest;
import com.quitmate.cigaretteHistoryBatch.service.response.CigaretteHistoryBatchIssueIgnoreResponse;
import com.quitmate.cigaretteHistoryBatch.service.response.CigaretteHistoryBatchIssueResponse;
import com.quitmate.global.exception.ConflictException;
import com.quitmate.global.exception.ForbiddenException;
import com.quitmate.global.exception.NotFoundException;
import com.quitmate.global.page.response.PageCustom;
import com.quitmate.user.users.entity.User;
import com.quitmate.user.users.entity.enums.Role;
import com.quitmate.user.users.entity.enums.SettingStatus;
import com.quitmate.user.users.entity.enums.SnsType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;

class CigaretteHistoryBatchIssueServiceTest extends IntegrationTestSupport {

    @Autowired
    private CigaretteHistoryBatchIssueService cigaretteHistoryBatchIssueService;

    @Autowired
    private CigaretteHistoryBatchIssueJpaRepository cigaretteHistoryBatchIssueJpaRepository;

    @AfterEach
    void deleteIssues() {
        cigaretteHistoryBatchIssueJpaRepository.deleteAllInBatch();
    }

    @DisplayName("처리 대기 목록에는 PENDING만, 오래된 날짜·사용자 ID 순으로 나온다")
    @Test
    void getPendingIssues() {
        // given
        loginAs(admin());
        save(issue("20261105", 30L, CigaretteHistoryBatchIssueStatus.PENDING));
        save(issue("20261103", 20L, CigaretteHistoryBatchIssueStatus.PENDING));
        save(issue("20261105", 10L, CigaretteHistoryBatchIssueStatus.PENDING));
        save(issue("20261101", 1L, CigaretteHistoryBatchIssueStatus.OPEN));
        save(issue("20261101", 2L, CigaretteHistoryBatchIssueStatus.RESOLVED));
        save(issue("20261101", 3L, CigaretteHistoryBatchIssueStatus.IGNORED));

        // when
        PageCustom<CigaretteHistoryBatchIssueResponse> page = cigaretteHistoryBatchIssueService.getPendingIssues(
                CigaretteHistoryBatchIssueListServiceRequest.builder().page(1).size(10).build());

        // then
        assertThat(page.getContent())
                .extracting("targetDate", "userId", "stage", "failCount", "lastErrorType")
                .containsExactly(
                        tuple("20261103", 20L, CigaretteHistoryBatchIssueStage.SMOKED_SAVE, 3, "com.mongodb.MongoException"),
                        tuple("20261105", 10L, CigaretteHistoryBatchIssueStage.SMOKED_SAVE, 3, "com.mongodb.MongoException"),
                        tuple("20261105", 30L, CigaretteHistoryBatchIssueStage.SMOKED_SAVE, 3, "com.mongodb.MongoException"));
        assertThat(page.getPageInfo().getTotalElement()).isEqualTo(3);
    }

    @DisplayName("[무시]하면 IGNORED가 되고 메모·처리한 관리자·처리 시각이 남으며, 목록에서 빠진다")
    @Test
    void ignore() {
        // given
        User admin = admin();
        loginAs(admin);
        CigaretteHistoryBatchIssue issue = save(issue("20261105", 10L, CigaretteHistoryBatchIssueStatus.PENDING));

        // when
        CigaretteHistoryBatchIssueIgnoreResponse response = cigaretteHistoryBatchIssueService.ignore(
                CigaretteHistoryBatchIssueIgnoreServiceRequest.builder()
                        .issueId(issue.getId())
                        .memo("  탈퇴한 사용자라 복구하지 않음  ")
                        .build());

        // then
        assertThat(response.getStatus()).isEqualTo(CigaretteHistoryBatchIssueStatus.IGNORED);
        assertThat(response.getMemo()).isEqualTo("탈퇴한 사용자라 복구하지 않음");
        assertThat(response.getResolvedBy()).isEqualTo(admin.getId());
        assertThat(response.getResolvedAt()).isNotNull();

        CigaretteHistoryBatchIssue saved = cigaretteHistoryBatchIssueJpaRepository.findById(issue.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(CigaretteHistoryBatchIssueStatus.IGNORED);
        assertThat(saved.getMemo()).isEqualTo("탈퇴한 사용자라 복구하지 않음");
        // 실패 기록은 그대로 둔다.
        assertThat(saved.getFailCount()).isEqualTo(3);

        assertThat(cigaretteHistoryBatchIssueService.getPendingIssues(
                CigaretteHistoryBatchIssueListServiceRequest.builder().page(1).size(10).build()).getContent())
                .isEmpty();
    }

    @DisplayName("이미 앱 서버가 닫은(RESOLVED) 건은 무시할 수 없고 409, 메모도 바뀌지 않는다")
    @Test
    void ignoreNotPending() {
        // given
        loginAs(admin());
        CigaretteHistoryBatchIssue issue = save(issue("20261105", 10L, CigaretteHistoryBatchIssueStatus.RESOLVED));

        // when // then
        assertThatThrownBy(() -> cigaretteHistoryBatchIssueService.ignore(
                CigaretteHistoryBatchIssueIgnoreServiceRequest.builder().issueId(issue.getId()).memo("메모").build()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("RESOLVED");
        CigaretteHistoryBatchIssue saved = cigaretteHistoryBatchIssueJpaRepository.findById(issue.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(CigaretteHistoryBatchIssueStatus.RESOLVED);
        assertThat(saved.getMemo()).isNull();
    }

    @DisplayName("없는 건을 무시하면 404")
    @Test
    void ignoreUnknown() {
        loginAs(admin());

        assertThatThrownBy(() -> cigaretteHistoryBatchIssueService.ignore(
                CigaretteHistoryBatchIssueIgnoreServiceRequest.builder().issueId(999_999L).memo("메모").build()))
                .isInstanceOf(NotFoundException.class);
    }

    @DisplayName("관리자가 아닌 계정은 목록 조회도 무시도 403")
    @Test
    void notAdmin() {
        // given
        User user = userRepository.save(createUser("user@test.com", "1234", SnsType.NORMAL, SettingStatus.COMPLETE));
        user.updateRole(Role.USER);
        userRepository.save(user);
        loginAs(user);
        CigaretteHistoryBatchIssue issue = save(issue("20261105", 10L, CigaretteHistoryBatchIssueStatus.PENDING));

        // when // then
        assertThatThrownBy(() -> cigaretteHistoryBatchIssueService.getPendingIssues(
                CigaretteHistoryBatchIssueListServiceRequest.builder().page(1).size(10).build()))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> cigaretteHistoryBatchIssueService.ignore(
                CigaretteHistoryBatchIssueIgnoreServiceRequest.builder().issueId(issue.getId()).memo("메모").build()))
                .isInstanceOf(ForbiddenException.class);
        assertThat(cigaretteHistoryBatchIssueJpaRepository.findById(issue.getId()).orElseThrow().getStatus())
                .isEqualTo(CigaretteHistoryBatchIssueStatus.PENDING);
    }

    private User admin() {
        return userRepository.save(createUser("admin@test.com", "1234", SnsType.NORMAL, SettingStatus.COMPLETE));
    }

    private void loginAs(User user) {
        given(securityService.getCurrentLoginUserInfo()).willReturn(createLoginUserInfo(user.getId()));
    }

    private CigaretteHistoryBatchIssue save(CigaretteHistoryBatchIssue issue) {
        return cigaretteHistoryBatchIssueJpaRepository.save(issue);
    }

    private static CigaretteHistoryBatchIssue issue(String targetDate, Long userId,
                                                    CigaretteHistoryBatchIssueStatus status) {
        LocalDateTime failedAt = LocalDateTime.of(2026, 11, 6, 0, 0);
        return CigaretteHistoryBatchIssue.builder()
                .targetDate(targetDate)
                .userId(userId)
                .stage(CigaretteHistoryBatchIssueStage.SMOKED_SAVE)
                .failCount(3)
                .status(status)
                .firstFailedAt(failedAt)
                .lastFailedAt(failedAt.plusDays(2))
                .lastErrorType("com.mongodb.MongoException")
                .lastAttemptId(5L)
                .build();
    }
}
