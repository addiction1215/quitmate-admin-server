package com.quitmate.cigaretteHistoryBatch.service.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 앱 서버 재처리(POST /api/v1/admin/cigarette-history-batch/runs) 결과. 앱 서버 응답 필드를 그대로 옮긴다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CigaretteHistoryBatchRunResponse {

    /** 이관한 날짜 yyyyMMdd */
    private String targetDate;
    /** 이번 실행 결과: DONE / FAILED / ABANDONED */
    private String status;
    /** 이 날짜의 몇 번째 실행인지 */
    private Integer attemptNo;
    /** 실행 계기. 관리자 재처리는 MANUAL */
    private String triggerType;
    private int insertedUserCount;
    private int skippedUserCount;
    private int failedUserCount;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
