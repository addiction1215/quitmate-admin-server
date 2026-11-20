package com.quitmate.challenge.challange.service.request;

import com.quitmate.challenge.challange.entity.Challenge;
import com.quitmate.challenge.mission.entity.Mission;
import com.quitmate.challenge.mission.entity.MissionCategoryStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MissionUpdateServiceRequest {

    private Long missionId;
    private MissionCategoryStatus category;
    private String title;
    private Integer reward;
    private String content;
    private Integer minHoldSeconds;
    /** 요청에 minHoldSeconds 칸이 있었는지. false면 기존 미션의 값을 그대로 둔다(새 미션은 비움). */
    private boolean minHoldSecondsSent;

    @Builder
    public MissionUpdateServiceRequest(Long missionId, MissionCategoryStatus category, String title, Integer reward, String content,
                                       Integer minHoldSeconds, boolean minHoldSecondsSent) {
        this.missionId = missionId;
        this.category = category;
        this.title = title;
        this.reward = reward;
        this.content = content;
        this.minHoldSeconds = minHoldSeconds;
        this.minHoldSecondsSent = minHoldSecondsSent;
    }

    public Mission toMissionEntity(Challenge challenge) {
        return Mission.builder()
                .challenge(challenge)
                .category(category)
                .title(title)
                .reward(reward)
                .content(content)
                .minHoldSeconds(minHoldSeconds)
                .build();
    }
}
