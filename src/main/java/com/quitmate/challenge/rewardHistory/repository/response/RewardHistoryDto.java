package com.quitmate.challenge.rewardHistory.repository.response;

import com.quitmate.challenge.rewardHistory.enums.RewardType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class RewardHistoryDto {
    private Long id;                        // 내역 ID
    private LocalDateTime createdDate;      // 발생일자
    private String userName;                // 유저명
    private RewardType type;                // 유형 (사용/획득)
    private Integer point;                  // 포인트 증감량
    private Integer remainingPoint;         // 남은 포인트

    public RewardHistoryDto(Long id, LocalDateTime createdDate, String userName,
                            RewardType type, Integer point, Integer remainingPoint) {
        this.id = id;
        this.createdDate = createdDate;
        this.userName = userName;
        this.type = type;
        this.point = point;
        this.remainingPoint = remainingPoint;
    }
}
