package com.quitmate.challenge.challengehistory.entity;

import com.quitmate.challenge.challange.entity.Challenge;
import com.quitmate.global.BaseTimeEntity;
import com.quitmate.user.users.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    /**
     * 앱 서버가 챌린지를 완료·포기한 시각(한국 시각, KAN-190 DDL의 completed_at·cancelled_at). 관리자 서버는 읽기만 한다.
     * 이 칸이 생기기 전에 끝난 챌린지는 비어 있어, 지표(KAN-194)는 그때 마지막 수정 시각(updatedDate)으로 대신한다.
     */
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;

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
