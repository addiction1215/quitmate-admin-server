package com.quitmate.challenge.challange.controller.request;

import com.quitmate.challenge.challange.service.request.ChallengeUpdateServiceRequest;
import com.quitmate.challenge.challange.service.request.MissionUpdateServiceRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.quitmate.challenge.mission.entity.MissionCategoryStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
public class ChallengeUpdateRequest {

    @NotBlank(message = "챌린지 제목은 필수입니다.")
    private String title;

    @NotBlank(message = "챌린지 뱃지는 필수입니다.")
    private String badge;

    @NotBlank(message = "챌린지 내용은 필수입니다.")
    private String content;

    @NotNull(message = "챌린지 리워드는 필수입니다.")
    private Integer reward;

    @NotEmpty(message = "최소 1개 이상의 미션이 필요합니다.")
    @Valid
    private List<MissionUpdateRequest> missions;

    @Builder
    public ChallengeUpdateRequest(String title, String badge, String content, Integer reward, List<MissionUpdateRequest> missions) {
        this.title = title;
        this.badge = badge;
        this.content = content;
        this.reward = reward;
        this.missions = missions;
    }

    public ChallengeUpdateServiceRequest toServiceRequest() {
        return ChallengeUpdateServiceRequest.builder()
                .title(title)
                .badge(badge)
                .content(content)
                .reward(reward)
                .missions(missions.stream()
                        .map(MissionUpdateRequest::toServiceRequest)
                        .collect(Collectors.toList()))
                .build();
    }

    @Getter
    @NoArgsConstructor
    public static class MissionUpdateRequest {

        private Long missionId;

        @NotNull(message = "미션 카테고리는 필수입니다.")
        private MissionCategoryStatus category;

        @NotBlank(message = "미션 제목은 필수입니다.")
        private String title;

        @NotNull(message = "미션 리워드는 필수입니다.")
        private Integer reward;

        @NotBlank(message = "미션 내용은 필수입니다.")
        private String content;

        /**
         * 참기(HOLD) 미션의 최소 참기 시간(초). 칸을 아예 보내지 않으면 기존 미션의 값을 그대로 두고(KAN-195 전 관리자 웹),
         * null을 보내면 앱 서버 기본값으로 돌린다. 참기가 아닌 미션은 보내도 저장하지 않는다.
         */
        @Min(value = MinHoldSecondsRule.MIN, message = MinHoldSecondsRule.RANGE_MESSAGE)
        @Max(value = MinHoldSecondsRule.MAX, message = MinHoldSecondsRule.RANGE_MESSAGE)
        private Integer minHoldSeconds;

        /** 요청 JSON에 minHoldSeconds 칸이 있었는지(null 포함). Jackson이 아래 setter를 부를 때만 true가 된다. */
        @JsonIgnore
        private boolean minHoldSecondsSent;

        /** 코드로 만들 때는 minHoldSeconds를 정한 것으로 본다(null이면 기본값으로 돌림). */
        @Builder
        public MissionUpdateRequest(Long missionId, MissionCategoryStatus category, String title, Integer reward, String content,
                                    Integer minHoldSeconds) {
            this.missionId = missionId;
            this.category = category;
            this.title = title;
            this.reward = reward;
            this.content = content;
            this.minHoldSeconds = minHoldSeconds;
            this.minHoldSecondsSent = true;
        }

        public void setMinHoldSeconds(Integer minHoldSeconds) {
            this.minHoldSeconds = minHoldSeconds;
            this.minHoldSecondsSent = true;
        }

        public MissionUpdateServiceRequest toServiceRequest() {
            return MissionUpdateServiceRequest.builder()
                    .missionId(missionId)
                    .category(category)
                    .title(title)
                    .reward(reward)
                    .content(content)
                    .minHoldSeconds(minHoldSeconds)
                    .minHoldSecondsSent(minHoldSecondsSent)
                    .build();
        }
    }
}
