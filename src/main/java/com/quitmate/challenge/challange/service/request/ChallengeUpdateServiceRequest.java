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
    private String content;
    private Integer reward;
    private List<MissionUpdateServiceRequest> missions;

    @Builder
    public ChallengeUpdateServiceRequest(String title, String badge, String content, Integer reward, List<MissionUpdateServiceRequest> missions) {
        this.title = title;
        this.badge = badge;
        this.content = content;
        this.reward = reward;
        this.missions = missions;
    }
}
