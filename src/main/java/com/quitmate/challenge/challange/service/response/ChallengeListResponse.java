package com.quitmate.challenge.challange.service.response;

import com.quitmate.challenge.challange.repository.response.ChallengeDto;
import lombok.Builder;
import lombok.Getter;

@Getter
public class ChallengeListResponse {

    private final Long challengeId;
    private final String title;
    private final String badge;
    private final Integer reward;
    private final Long missionCount;
    private final Long accumulatedCount;

    @Builder
    public ChallengeListResponse(
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

    public static ChallengeListResponse createResponse(ChallengeDto dto, String badge) {
        return ChallengeListResponse.builder()
                .challengeId(dto.getChallengeId())
                .title(dto.getTitle())
                .badge(badge)
                .reward(dto.getReward())
                .missionCount(dto.getMissionCount())
                .accumulatedCount(dto.getAccumulatedCount())
                .build();
    }
}
