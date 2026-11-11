package com.quitmate.cigaretteHistoryBatch.enums;

/**
 * 어느 저장에서 실패했는지. 앱 서버(addiction)와 같은 값을 쓴다.
 */
public enum CigaretteHistoryBatchIssueStage {
    /** 흡연 기록이 있는 날의 실데이터 저장 */
    SMOKED_SAVE,
    /** 흡연 기록이 없는 날의 0건 문서 저장 */
    ZERO_SAVE
}
