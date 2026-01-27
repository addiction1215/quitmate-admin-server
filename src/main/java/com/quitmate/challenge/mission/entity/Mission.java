package com.quitmate.challenge.mission.entity;

import com.quitmate.challenge.challange.entity.Challenge;
import com.quitmate.global.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Mission extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Challenge challenge;

    @Enumerated(EnumType.STRING)
    private MissionCategoryStatus category;

    private String title;

    private Integer reward;

    private String content;

    @Builder
    public Mission(Long id, Challenge challenge, MissionCategoryStatus category, String title, Integer reward, String content) {
        this.id = id;
        this.challenge = challenge;
        this.category = category;
        this.title = title;
        this.reward = reward;
        this.content = content;
    }
}
