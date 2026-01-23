package com.quitmate.challenge.challange.entity;

import com.quitmate.global.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Challenge extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String content;

    private String badge;

    private Integer reward;

    @Builder
    public Challenge(Long id, String title, String content, String badge, Integer reward) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.badge = badge;
        this.reward = reward;
    }
}
