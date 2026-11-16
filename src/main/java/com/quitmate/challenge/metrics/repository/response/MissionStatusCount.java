package com.quitmate.challenge.metrics.repository.response;

import com.quitmate.challenge.missionhistory.entity.MissionStatus;

/**
 * 미션·상태별 미션 기록 수.
 */
public record MissionStatusCount(Long missionId, MissionStatus status, Long count) {
}
