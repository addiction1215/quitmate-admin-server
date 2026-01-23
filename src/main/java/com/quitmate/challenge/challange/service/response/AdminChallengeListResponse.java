package com.quitmate.challenge.challange.service.response;

import lombok.Builder;
import lombok.Getter;

@Getter
public class AdminChallengeListResponse {
    
    private final Long challengeId;
    private final String title;
    private final String badge;
    private final Integer reward;
    private final Long missionCount;
    private final Long accumulatedCount;
    
    @Builder
    public AdminChallengeListResponse(
            Long challengeId,
            String title,
            String badge,
            Integer reward,
            Long missionCount,
            Long accumulatedCount) {
        this.challengeId = challengeId;
        this.title = title;
        this.badge = badge;
        this.reward = reward;
        this.missionCount = missionCount;
        this.accumulatedCount = accumulatedCount;
    }
}
