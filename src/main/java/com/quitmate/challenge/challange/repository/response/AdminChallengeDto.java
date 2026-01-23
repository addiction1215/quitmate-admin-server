package com.quitmate.challenge.challange.repository.response;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

@Getter
public class AdminChallengeDto {
    
    private final Long challengeId;
    private final String title;
    private final String badge;
    private final Integer reward;
    private final Long missionCount;
    private final Long accumulatedCount;
    
    @QueryProjection
    public AdminChallengeDto(
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
        this.missionCount = missionCount != null ? missionCount : 0L;
        this.accumulatedCount = accumulatedCount != null ? accumulatedCount : 0L;
    }
}
