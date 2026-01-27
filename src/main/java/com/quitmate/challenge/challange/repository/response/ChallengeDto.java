package com.quitmate.challenge.challange.repository.response;

import lombok.Getter;

@Getter
public class ChallengeDto {

    private Long challengeId;
    private String title;
    private String badge;
    private Integer reward;
    private Long missionCount;
    private Long accumulatedCount;

    public ChallengeDto(Long challengeId, String title, String badge,
                        Integer reward, Long missionCount, Long accumulatedCount) {
        this.challengeId = challengeId;
        this.title = title;
        this.badge = badge;
        this.reward = reward;
        this.missionCount = missionCount != null ? missionCount : 0L;
        this.accumulatedCount = accumulatedCount != null ? accumulatedCount : 0L;
    }
}
