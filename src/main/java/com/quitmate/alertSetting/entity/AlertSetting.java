package com.quitmate.alertSetting.entity;

import com.quitmate.alertSetting.entity.enums.AlertType;
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
public class AlertSetting extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "all_alerts")
    private AlertType all;

    @Enumerated(EnumType.STRING)
    private AlertType smokingWarning;

    @Enumerated(EnumType.STRING)
    private AlertType leaderboardRank;

    @Enumerated(EnumType.STRING)
    private AlertType challenge;

    @Enumerated(EnumType.STRING)
    private AlertType report;

    @Builder
    private AlertSetting(User user, AlertType all, AlertType smokingWarning,
                         AlertType leaderboardRank, AlertType challenge, AlertType report) {
        this.user = user;
        this.all = all;
        this.smokingWarning = smokingWarning;
        this.leaderboardRank = leaderboardRank;
        this.challenge = challenge;
        this.report = report;
    }

    public static AlertSetting createDefault(User user) {
        return AlertSetting.builder()
                .user(user)
                .all(AlertType.ON)
                .smokingWarning(AlertType.ON)
                .leaderboardRank(AlertType.ON)
                .challenge(AlertType.ON)
                .report(AlertType.ON)
                .build();
    }
}
