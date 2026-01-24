package com.quitmate.challenge.challange.service.request;

import com.quitmate.challenge.challange.entity.Challenge;
import com.quitmate.challenge.mission.entity.Mission;
import com.quitmate.challenge.mission.entity.MissionCategoryStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MissionCreateServiceRequest {

    private MissionCategoryStatus category;
    private String title;
    private Integer reward;
    private String content;

    @Builder
    public MissionCreateServiceRequest(MissionCategoryStatus category, String title, Integer reward, String content) {
        this.category = category;
        this.title = title;
        this.reward = reward;
        this.content = content;
    }

    public Mission toMissionEntity(Challenge challenge) {
        return Mission.builder()
                .challenge(challenge)
                .category(category)
                .title(title)
                .reward(reward)
                .content(content)
                .build();
    }
}
