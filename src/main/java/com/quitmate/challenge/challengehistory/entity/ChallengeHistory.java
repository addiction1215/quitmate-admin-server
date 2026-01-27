package com.quitmate.challenge.challengehistory.entity;

import com.quitmate.challenge.challange.entity.Challenge;
import com.quitmate.global.BaseTimeEntity;
import com.quitmate.user.users.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class ChallengeHistory extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Enumerated(EnumType.STRING)
    private ChallengeStatus status;

    @Builder
    public ChallengeHistory(Long id, Challenge challenge, User user, ChallengeStatus status) {
        this.id = id;
        this.challenge = challenge;
        this.user = user;
        this.status = status;
    }

    public void updateStatus(ChallengeStatus status) {
        this.status = status;
    }
}
