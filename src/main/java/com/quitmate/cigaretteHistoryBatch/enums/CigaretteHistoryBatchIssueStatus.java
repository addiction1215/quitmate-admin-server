package com.quitmate.cigaretteHistoryBatch.enums;

/**
 * 이관에 반복해서 실패한 (날짜, 사용자)의 처리 상태. 앱 서버(addiction)와 같은 값을 쓴다.
 */
public enum CigaretteHistoryBatchIssueStatus {
    /** 자동 재시도 중 */
    OPEN,
    /** 실패가 K회 쌓여 자동 보정에서 빠지고 사람이 처리해야 함. 「이관 처리 대기」 목록에 나온다. */
    PENDING,
    /** 이후 실행에서 저장에 성공함 */
    RESOLVED,
    /** 관리자가 복구하지 않기로 함 */
    IGNORED
}
