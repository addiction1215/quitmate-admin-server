package com.quitmate.challenge.challange.service.request;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ChallengeUpdateServiceRequest {

    private String title;
    private String badge;
    private Integer reward;
    private List<MissionUpdateServiceRequest> missions;

    @Builder
    public ChallengeUpdateServiceRequest(String title, String badge, Integer reward, List<MissionUpdateServiceRequest> missions) {
        this.title = title;
        this.badge = badge;
        this.reward = reward;
        this.missions = missions;
    }
}
