package com.quitmate.cigaretteHistoryBatch.controller;

import com.quitmate.cigaretteHistoryBatch.controller.request.CigaretteHistoryBatchIssueIgnoreRequest;
import com.quitmate.cigaretteHistoryBatch.controller.request.CigaretteHistoryBatchIssueListRequest;
import com.quitmate.cigaretteHistoryBatch.controller.request.CigaretteHistoryBatchRunRequest;
import com.quitmate.cigaretteHistoryBatch.service.CigaretteHistoryBatchIssueService;
import com.quitmate.cigaretteHistoryBatch.service.CigaretteHistoryBatchRunService;
import com.quitmate.cigaretteHistoryBatch.service.response.CigaretteHistoryBatchAttemptResponse;
import com.quitmate.cigaretteHistoryBatch.service.response.CigaretteHistoryBatchDateStatusResponse;
import com.quitmate.cigaretteHistoryBatch.service.response.CigaretteHistoryBatchIssueIgnoreResponse;
import com.quitmate.cigaretteHistoryBatch.service.response.CigaretteHistoryBatchIssueResponse;
import com.quitmate.cigaretteHistoryBatch.service.response.CigaretteHistoryBatchRunResponse;
import com.quitmate.global.ApiResponse;
import com.quitmate.global.page.response.PageCustom;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 어드민 「흡연 기록 이관」 화면(KAN-89). 관리자 계정만 쓸 수 있다(서비스에서 DB로 확인).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/cigarette-history-batch")
public class CigaretteHistoryBatchController {

    private final CigaretteHistoryBatchIssueService cigaretteHistoryBatchIssueService;
    private final CigaretteHistoryBatchRunService cigaretteHistoryBatchRunService;

    /** 처리 대기(PENDING) 목록 */
    @GetMapping("/issues")
    public ApiResponse<PageCustom<CigaretteHistoryBatchIssueResponse>> getPendingIssues(
            @Valid @ModelAttribute CigaretteHistoryBatchIssueListRequest request) {
        return ApiResponse.ok(cigaretteHistoryBatchIssueService.getPendingIssues(request.toServiceRequest()));
    }

    /** [무시]: 메모를 남기고 IGNORED로 닫는다 */
    @PatchMapping("/issues/{issueId}/ignore")
    public ApiResponse<CigaretteHistoryBatchIssueIgnoreResponse> ignoreIssue(
            @PathVariable("issueId") Long issueId,
            @Valid @RequestBody CigaretteHistoryBatchIssueIgnoreRequest request) {
        return ApiResponse.ok(cigaretteHistoryBatchIssueService.ignore(request.toServiceRequest(issueId)));
    }

    /** [재처리]: 앱 서버에 그 날짜 재이관을 요청한다(force=false) */
    @PostMapping("/runs")
    public ApiResponse<CigaretteHistoryBatchRunResponse> run(
            @Valid @RequestBody CigaretteHistoryBatchRunRequest request) {
        return ApiResponse.ok(cigaretteHistoryBatchRunService.run(request.toServiceRequest()));
    }

    /** 날짜별 배치 상태 */
    @GetMapping("/runs")
    public ApiResponse<List<CigaretteHistoryBatchDateStatusResponse>> getDateStatuses(
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to) {
        return ApiResponse.ok(cigaretteHistoryBatchRunService.getDateStatuses(from, to));
    }

    /** 날짜별 실행 이력 */
    @GetMapping("/runs/{targetDate}/attempts")
    public ApiResponse<List<CigaretteHistoryBatchAttemptResponse>> getAttempts(
            @PathVariable("targetDate") String targetDate) {
        return ApiResponse.ok(cigaretteHistoryBatchRunService.getAttempts(targetDate));
    }
}
