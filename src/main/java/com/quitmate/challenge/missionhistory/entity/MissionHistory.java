package com.quitmate.challenge.missionhistory.entity;

import com.quitmate.challenge.challengehistory.entity.ChallengeHistory;
import com.quitmate.challenge.mission.entity.Mission;
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
public class MissionHistory extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private ChallengeHistory challengeHistory;

    @ManyToOne(fetch = FetchType.LAZY)
    private Mission mission;

    @Enumerated(EnumType.STRING)
    private MissionStatus status;

    private LocalDateTime completeAt;

    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    // 미션 제출 관련 컬럼들
    private Integer gpsVerifyCount;     // GPS 검증 횟수
    private String photoUrl1;           // 사진 URL 1
    private String photoUrl2;           // 사진 URL 2
    private String photoUrl3;           // 사진 URL 3
    private Integer abstinenceTime;     // 금연 시간 (초 단위)

    @Builder
    public MissionHistory(Long id, Mission mission, ChallengeHistory challengeHistory, MissionStatus status,
                          LocalDateTime completeAt, String address, User user,
                          Integer gpsVerifyCount, String photoUrl1, String photoUrl2, String photoUrl3, Integer abstinenceTime) {
        this.id = id;
        this.mission = mission;
        this.challengeHistory = challengeHistory;
        this.status = status;
        this.completeAt = completeAt;
        this.address = address;
        this.user = user;
        this.gpsVerifyCount = gpsVerifyCount;
        this.photoUrl1 = photoUrl1;
        this.photoUrl2 = photoUrl2;
        this.photoUrl3 = photoUrl3;
        this.abstinenceTime = abstinenceTime;
    }

}
