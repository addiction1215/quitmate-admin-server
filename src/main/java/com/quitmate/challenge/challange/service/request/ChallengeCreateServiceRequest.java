package com.quitmate.challenge.challange.service.request;

import com.quitmate.challenge.challange.entity.Challenge;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ChallengeCreateServiceRequest {

    private String title;
    private String badge;
    private Integer reward;
    private List<MissionCreateServiceRequest> missions;

    @Builder
    public ChallengeCreateServiceRequest(String title, String badge, Integer reward, List<MissionCreateServiceRequest> missions) {
        this.title = title;
        this.badge = badge;
        this.reward = reward;
        this.missions = missions;
    }

    public Challenge toChallengeEntity() {
        return Challenge.builder()
                .title(title)
                .badge(badge)
                .reward(reward)
                .build();
    }
}
