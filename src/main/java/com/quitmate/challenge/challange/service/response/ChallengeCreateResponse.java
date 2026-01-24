package com.quitmate.challenge.challange.service.response;

import com.quitmate.challenge.challange.entity.Challenge;
import lombok.Builder;
import lombok.Getter;

@Getter
public class ChallengeCreateResponse {

    private final Long challengeId;
    private final String title;
    private final String badge;
    private final Integer reward;
    private final Integer missionCount;

    @Builder
    private ChallengeCreateResponse(Long challengeId, String title, String badge, Integer reward, Integer missionCount) {
        this.challengeId = challengeId;
        this.title = title;
        this.badge = badge;
        this.reward = reward;
        this.missionCount = missionCount;
    }

    public static ChallengeCreateResponse createResponse(Challenge challenge, int missionCount) {
        return ChallengeCreateResponse.builder()
                .challengeId(challenge.getId())
                .title(challenge.getTitle())
                .badge(challenge.getBadge())
                .reward(challenge.getReward())
                .missionCount(missionCount)
                .build();
    }
}
