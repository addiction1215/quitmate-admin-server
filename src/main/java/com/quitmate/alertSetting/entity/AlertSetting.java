package com.quitmate.alertSetting.entity;

import com.quitmate.alertSetting.entity.enums.AdPushConsentStatus;
import com.quitmate.alertSetting.entity.enums.AlertType;
import com.quitmate.global.BaseTimeEntity;
import com.quitmate.user.users.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 앱 서버 {@code AlertSetting}의 복제 엔티티. 같은 {@code alert_setting} 테이블을 쓴다.
 *
 * <p>KAN-107에서 앱 서버가 더한 칸(서비스 소식·금연 활동·광고성 앱 푸시 동의)을 이름·타입까지 같게 매핑한다.
 * 이벤트·혜택은 따로 칸이 없고 {@code ad_push_consent_status = 'AGREED'}로 판단한다.
 * 관리자 서버는 이 칸들을 바꾸지 않는다. {@code @DynamicUpdate}는 혹시 다른 칸을 고쳐도 앱 서버가 쓴 값을 덮어쓰지 않게 둔다.</p>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@DynamicUpdate
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

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "service_news", nullable = false, length = 10)
    private AlertType serviceNews;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "smoke_free_activity", nullable = false, length = 10)
    private AlertType smokeFreeActivity;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "ad_push_consent_status", nullable = false, length = 20)
    private AdPushConsentStatus adPushConsentStatus;

    @Column(name = "ad_push_consent_changed_at")
    private LocalDateTime adPushConsentChangedAt;

    @Builder
    private AlertSetting(User user, AlertType all, AlertType smokingWarning,
                         AlertType leaderboardRank, AlertType challenge, AlertType report) {
        this.user = user;
        this.all = all;
        this.smokingWarning = smokingWarning;
        this.leaderboardRank = leaderboardRank;
        this.challenge = challenge;
        this.report = report;
        // 앱 서버와 같은 기본값. 문의 답변 푸시가 행을 새로 만들 때 NOT NULL 칸이 비지 않게 한다.
        this.serviceNews = AlertType.ON;
        this.smokeFreeActivity = AlertType.ON;
        this.adPushConsentStatus = AdPushConsentStatus.NOT_AGREED;
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

    /** 서비스 소식(안내·업데이트·점검 공지) 수신 여부 */
    public boolean receivesServiceNews() {
        return this.serviceNews == AlertType.ON;
    }

    /** 이벤트·혜택(광고성 앱 푸시) 수신 여부. 동의(AGREED)한 사람만. 미동의·철회는 받지 않는다 */
    public boolean agreedToAdPush() {
        return this.adPushConsentStatus == AdPushConsentStatus.AGREED;
    }
}
