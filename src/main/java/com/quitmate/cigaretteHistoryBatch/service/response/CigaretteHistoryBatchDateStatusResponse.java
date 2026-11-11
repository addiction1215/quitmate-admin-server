package com.quitmate.cigaretteHistoryBatch.service.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 앱 서버 기간별 날짜 상태(GET .../runs?from=&to=) 한 줄. 실행 기록이 없는 날짜는 status가 null이다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CigaretteHistoryBatchDateStatusResponse {

    private String targetDate;
    /** RUNNING / DONE / FAILED. 한 번도 실행되지 않은 날짜는 null */
    private String status;
    private int attemptCount;
    private String lastTriggerType;
    private Integer totalUserCount;
    private Integer successUserCount;
    private Integer failedUserCount;
    private String lastErrorSummary;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
