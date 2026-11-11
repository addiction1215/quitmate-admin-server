package com.quitmate.cigaretteHistoryBatch.service;

import com.quitmate.cigaretteHistoryBatch.entity.CigaretteHistoryBatchIssue;
import com.quitmate.cigaretteHistoryBatch.enums.CigaretteHistoryBatchIssueStatus;
import com.quitmate.cigaretteHistoryBatch.repository.CigaretteHistoryBatchIssueRepository;
import com.quitmate.cigaretteHistoryBatch.service.request.CigaretteHistoryBatchIssueIgnoreServiceRequest;
import com.quitmate.cigaretteHistoryBatch.service.request.CigaretteHistoryBatchIssueListServiceRequest;
import com.quitmate.cigaretteHistoryBatch.service.response.CigaretteHistoryBatchIssueIgnoreResponse;
import com.quitmate.cigaretteHistoryBatch.service.response.CigaretteHistoryBatchIssueResponse;
import com.quitmate.global.exception.ConflictException;
import com.quitmate.global.exception.NotFoundException;
import com.quitmate.global.page.response.PageCustom;
import com.quitmate.global.security.AdminAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 「이관 처리 대기」 목록과 [무시]. 행은 같은 DB를 쓰는 앱 서버 이관 배치가 만든다.
 *
 * <p>[무시]는 issue 행의 상태와 메모만 바꾼다(KAN-73 댓글 10263, 관리자 서버에서 처리).
 * MySQL에 남은 흡연 원본(user_cigarette)은 지우지 않는다. 지우면 되돌릴 수 없고, 그 날짜를 다시 돌릴 때
 * 피운 사람에게 0건(금연) 문서가 생기기 때문이다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CigaretteHistoryBatchIssueService {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final String UNKNOWN_ISSUE = "해당 이관 처리 대기 건이 없습니다.";

    private final CigaretteHistoryBatchIssueRepository cigaretteHistoryBatchIssueRepository;
    private final AdminAccessService adminAccessService;

    @Transactional(readOnly = true)
    public PageCustom<CigaretteHistoryBatchIssueResponse> getPendingIssues(
            CigaretteHistoryBatchIssueListServiceRequest request) {
        adminAccessService.getCurrentAdminId();
        return PageCustom.of(cigaretteHistoryBatchIssueRepository
                .findAllByStatus(CigaretteHistoryBatchIssueStatus.PENDING, request.toPageable())
                .map(CigaretteHistoryBatchIssueResponse::of));
    }

    /**
     * PENDING 건을 IGNORED로 닫고 메모를 남긴다. 그 사이 앱 서버가 먼저 닫았으면 409.
     */
    public CigaretteHistoryBatchIssueIgnoreResponse ignore(CigaretteHistoryBatchIssueIgnoreServiceRequest request) {
        Long adminId = adminAccessService.getCurrentAdminId();
        int updated = cigaretteHistoryBatchIssueRepository.ignoreIfPending(request.getIssueId(),
                request.getMemo().trim(), adminId, LocalDateTime.now(KOREA));

        CigaretteHistoryBatchIssue issue = cigaretteHistoryBatchIssueRepository.findById(request.getIssueId())
                .orElseThrow(() -> new NotFoundException(UNKNOWN_ISSUE));
        if (updated == 0) {
            throw new ConflictException("처리 대기 상태가 아니라 무시할 수 없습니다. (현재 상태: " + issue.getStatus() + ")");
        }
        return CigaretteHistoryBatchIssueIgnoreResponse.of(issue);
    }
}
