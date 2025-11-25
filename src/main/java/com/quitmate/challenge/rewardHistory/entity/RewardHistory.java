package com.quitmate.challenge.rewardHistory.entity;

import com.quitmate.challenge.rewardHistory.enums.RewardType;
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
public class RewardHistory extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private RewardType type;

    private Integer point;

    private Integer remainingPoint;

    @Builder
    public RewardHistory(Long id, RewardType type, Integer point, Integer remainingPoint, User user) {
        this.id = id;
        this.type = type;
        this.point = point;
        this.remainingPoint = remainingPoint;
        this.user = user;
    }
}
