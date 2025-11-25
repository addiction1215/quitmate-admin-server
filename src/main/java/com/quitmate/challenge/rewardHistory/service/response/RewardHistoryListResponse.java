package com.quitmate.challenge.rewardHistory.service.response;

import com.quitmate.challenge.rewardHistory.enums.RewardType;
import com.quitmate.challenge.rewardHistory.repository.response.RewardHistoryDto;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
public class RewardHistoryListResponse {

    private final Long id;                        // 내역 ID
    private final LocalDateTime createdDate;      // 발생일자
    private final String userName;                // 유저명
    private final RewardType type;                // 유형 (사용/획득)
    private final Integer point;                  // 포인트 증감량
    private final Integer remainingPoint;         // 남은 포인트

    @Builder
    private RewardHistoryListResponse(Long id, LocalDateTime createdDate, String userName,
                                      RewardType type, Integer point, Integer remainingPoint) {
        this.id = id;
        this.createdDate = createdDate;
        this.userName = userName;
        this.type = type;
        this.point = point;
        this.remainingPoint = remainingPoint;
    }

    public static RewardHistoryListResponse createResponse(RewardHistoryDto dto) {
        return RewardHistoryListResponse.builder()
                .id(dto.getId())
                .createdDate(dto.getCreatedDate())
                .userName(dto.getUserName())
                .type(dto.getType())
                .point(dto.getPoint())
                .remainingPoint(dto.getRemainingPoint())
                .build();
    }
}
