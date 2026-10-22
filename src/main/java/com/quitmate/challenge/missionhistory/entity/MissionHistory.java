package com.quitmate.challenge.missionhistory.entity;

import com.quitmate.challenge.challengehistory.entity.ChallengeHistory;
import com.quitmate.challenge.mission.entity.Mission;
import com.quitmate.global.BaseTimeEntity;
import com.quitmate.global.exception.QuitmateException;
import com.quitmate.user.users.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @ElementCollection
    @CollectionTable(name = "mission_history_address", joinColumns = @JoinColumn(name = "mission_history_id"))
    @Column(name = "address")
    private List<String> addresses = new ArrayList<>();

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
                          LocalDateTime completeAt, List<String> addresses, User user,
                          Integer gpsVerifyCount, String photoUrl1, String photoUrl2, String photoUrl3, Integer abstinenceTime) {
        this.id = id;
        this.mission = mission;
        this.challengeHistory = challengeHistory;
        this.status = status;
        this.completeAt = completeAt;
        if (addresses != null) {
            this.addresses.addAll(addresses);
        }
        this.user = user;
        this.gpsVerifyCount = gpsVerifyCount;
        this.photoUrl1 = photoUrl1;
        this.photoUrl2 = photoUrl2;
        this.photoUrl3 = photoUrl3;
        this.abstinenceTime = abstinenceTime;
    }

    /**
     * 관리자 심사 결과를 반영한다. 승인 대기(READY) 미션만 심사할 수 있다.
     * 반려하면 제출 내용을 비워 사용자가 처음부터 다시 제출할 수 있게 하고,
     * 상태는 FAILED로 남겨 앱이 반려 안내를 띄울 수 있게 한다.
     */
    public void judge(boolean isSuccess) {
        if (this.status != MissionStatus.READY) {
            throw new QuitmateException("승인 대기 중인 미션만 심사할 수 있습니다.");
        }

        if (isSuccess) {
            this.status = MissionStatus.COMPLETED;
            this.completeAt = LocalDateTime.now();
            return;
        }

        this.status = MissionStatus.FAILED;
        clearSubmission();
    }

    private void clearSubmission() {
        this.addresses.clear();
        this.gpsVerifyCount = 0;
        this.photoUrl1 = null;
        this.photoUrl2 = null;
        this.photoUrl3 = null;
        this.abstinenceTime = 0;
    }
}
