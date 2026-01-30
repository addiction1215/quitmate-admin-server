package com.quitmate.challenge.challange.service.response;

import com.quitmate.challenge.challange.entity.Challenge;
import com.quitmate.challenge.mission.entity.Mission;
import com.quitmate.challenge.mission.entity.MissionCategoryStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class ChallengeDetailResponse {

    private final Long challengeId;
    private final String title;
    private final String badge;
    private final Integer reward;
    private final List<MissionResponse> missions;

    @Builder
    private ChallengeDetailResponse(Long challengeId, String title, String badge, Integer reward, List<MissionResponse> missions) {
        this.challengeId = challengeId;
        this.title = title;
        this.badge = badge;
        this.reward = reward;
        this.missions = missions;
    }

    public static ChallengeDetailResponse createResponse(Challenge challenge, List<Mission> missions, String badge) {
        return ChallengeDetailResponse.builder()
                .challengeId(challenge.getId())
                .title(challenge.getTitle())
                .badge(badge)
                .reward(challenge.getReward())
                .missions(missions.stream()
                        .map(MissionResponse::createResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    @Getter
    public static class MissionResponse {
        private final Long missionId;
        private final MissionCategoryStatus category;
        private final String title;
        private final Integer reward;
        private final String content;

        @Builder
        private MissionResponse(Long missionId, MissionCategoryStatus category, String title, Integer reward, String content) {
            this.missionId = missionId;
            this.category = category;
            this.title = title;
            this.reward = reward;
            this.content = content;
        }

        public static MissionResponse createResponse(Mission mission) {
            return MissionResponse.builder()
                    .missionId(mission.getId())
                    .category(mission.getCategory())
                    .title(mission.getTitle())
                    .reward(mission.getReward())
                    .content(mission.getContent())
                    .build();
        }
    }
}
