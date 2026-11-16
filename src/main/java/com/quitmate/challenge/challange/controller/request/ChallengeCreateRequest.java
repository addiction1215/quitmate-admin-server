package com.quitmate.challenge.challange.controller.request;

import com.quitmate.challenge.challange.service.request.ChallengeCreateServiceRequest;
import com.quitmate.challenge.challange.service.request.MissionCreateServiceRequest;
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
public class ChallengeCreateRequest {

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
    private List<MissionRequest> missions;

    @Builder
    public ChallengeCreateRequest(String title, String badge, String content, Integer reward, List<MissionRequest> missions) {
        this.title = title;
        this.badge = badge;
        this.content = content;
        this.reward = reward;
        this.missions = missions;
    }

    public ChallengeCreateServiceRequest toServiceRequest() {
        return ChallengeCreateServiceRequest.builder()
                .title(title)
                .badge(badge)
                .content(content)
                .reward(reward)
                .missions(missions.stream()
                        .map(MissionRequest::toServiceRequest)
                        .collect(Collectors.toList()))
                .build();
    }

    @Getter
    @NoArgsConstructor
    public static class MissionRequest {

        @NotNull(message = "미션 카테고리는 필수입니다.")
        private MissionCategoryStatus category;

        @NotBlank(message = "미션 제목은 필수입니다.")
        private String title;

        @NotNull(message = "미션 리워드는 필수입니다.")
        private Integer reward;

        @NotBlank(message = "미션 내용은 필수입니다.")
        private String content;

        /** 참기(HOLD) 미션의 최소 참기 시간(초). 비우면 앱 서버 기본값. 참기가 아닌 미션은 보내도 저장하지 않는다. */
        @Min(value = MinHoldSecondsRule.MIN, message = MinHoldSecondsRule.RANGE_MESSAGE)
        @Max(value = MinHoldSecondsRule.MAX, message = MinHoldSecondsRule.RANGE_MESSAGE)
        private Integer minHoldSeconds;

        @Builder
        public MissionRequest(MissionCategoryStatus category, String title, Integer reward, String content,
                              Integer minHoldSeconds) {
            this.category = category;
            this.title = title;
            this.reward = reward;
            this.content = content;
            this.minHoldSeconds = minHoldSeconds;
        }

        public MissionCreateServiceRequest toServiceRequest() {
            return MissionCreateServiceRequest.builder()
                    .category(category)
                    .title(title)
                    .reward(reward)
                    .content(content)
                    .minHoldSeconds(minHoldSeconds)
                    .build();
        }
    }
}
