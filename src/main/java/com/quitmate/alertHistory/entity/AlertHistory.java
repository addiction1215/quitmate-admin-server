package com.quitmate.alertHistory.entity;

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
public class AlertHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private String alertDescription;

    @Enumerated(EnumType.STRING)
    private AlertHistoryStatus alertHistoryStatus;

    @Enumerated(EnumType.STRING)
    private AlertDestinationType alertDestinationType;

    private String alertDestinationInfo;

    @Builder
    private AlertHistory(User user, String alertDescription, AlertHistoryStatus alertHistoryStatus,
                         AlertDestinationType alertDestinationType, String alertDestinationInfo) {
        this.user = user;
        this.alertDescription = alertDescription;
        this.alertHistoryStatus = alertHistoryStatus;
        this.alertDestinationType = alertDestinationType;
        this.alertDestinationInfo = alertDestinationInfo;
    }

    public void updateAlertHistoryStatus(AlertHistoryStatus alertHistoryStatus) {
        this.alertHistoryStatus = alertHistoryStatus;
    }
}
