package com.quitmate.challenge.missionhistory.service.response;

import com.quitmate.challenge.mission.entity.MissionCategoryStatus;
import com.quitmate.challenge.missionhistory.entity.MissionStatus;
import com.quitmate.challenge.missionhistory.repository.response.MissionHistoryDto;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MissionHistoryListResponse {
    private final Long missionHistoryId;
    private final String challengeTitle;
    private final String missionTitle;
    private final MissionCategoryStatus category;
    private final MissionStatus status;
    private final String userName;
    private final LocalDateTime createdDate;

    @Builder
    private MissionHistoryListResponse(Long missionHistoryId, String challengeTitle, String missionTitle,
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

    public static MissionHistoryListResponse createResponse(MissionHistoryDto dto) {
        return MissionHistoryListResponse.builder()
                .missionHistoryId(dto.getMissionHistoryId())
                .challengeTitle(dto.getChallengeTitle())
                .missionTitle(dto.getMissionTitle())
                .category(dto.getCategory())
                .status(dto.getStatus())
                .userName(dto.getUserName())
                .createdDate(dto.getCreatedDate())
                .build();
    }
}
