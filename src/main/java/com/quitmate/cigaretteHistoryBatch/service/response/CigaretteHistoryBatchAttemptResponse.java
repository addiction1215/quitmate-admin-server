package com.quitmate.cigaretteHistoryBatch.service.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 앱 서버 날짜별 실행 이력(GET .../runs/{targetDate}/attempts) 한 줄.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CigaretteHistoryBatchAttemptResponse {

    private int attemptNo;
    /** SCHEDULED / CATCH_UP / MANUAL */
    private String triggerType;
    /** 수동 실행한 관리자 userId */
    private Long requestedBy;
    /** 실행 서버 호스트명:PID:UUID8 */
    private String executor;
    private String activeProfile;
    /** RUNNING / DONE / FAILED / ABANDONED */
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Integer sourceRowCount;
    private Integer totalUserCount;
    private Integer insertedUserCount;
    private Integer skippedUserCount;
    private Integer failedUserCount;
    private Integer retriedUserCount;
    private Integer deletedSourceRowCount;
    private String errorType;
    private String errorMessage;
}
