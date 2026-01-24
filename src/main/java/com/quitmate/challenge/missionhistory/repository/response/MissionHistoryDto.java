package com.quitmate.challenge.missionhistory.repository.response;

import com.quitmate.challenge.mission.entity.MissionCategoryStatus;
import com.quitmate.challenge.missionhistory.entity.MissionStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MissionHistoryDto {
    private Long missionHistoryId;          // 요청 ID
    private String challengeTitle;           // 챌린지 제목
    private String missionTitle;             // 미션 제목
    private MissionCategoryStatus category;  // 미션 유형
    private MissionStatus status;            // 처리 여부
    private String userName;                 // 사용자 이름
    private LocalDateTime createdDate;       // 요청 일시

    public MissionHistoryDto(Long missionHistoryId, String challengeTitle, String missionTitle,
                             MissionCategoryStatus category, MissionStatus status,
                             String userName, LocalDateTime createdDate) {
        this.missionHistoryId = missionHistoryId;
        this.challengeTitle = challengeTitle;
        this.missionTitle = missionTitle;
        this.category = category;
        this.status = status;
        this.userName = userName;
        this.createdDate = createdDate;
    }
}
