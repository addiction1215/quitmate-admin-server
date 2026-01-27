package com.quitmate.challenge.challange.controller.request;

import com.quitmate.challenge.challange.service.request.ChallengeUpdateServiceRequest;
import com.quitmate.challenge.challange.service.request.MissionUpdateServiceRequest;
import com.quitmate.challenge.mission.entity.MissionCategoryStatus;
import jakarta.validation.Valid;
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

    @NotNull(message = "챌린지 리워드는 필수입니다.")
    private Integer reward;

    @NotEmpty(message = "최소 1개 이상의 미션이 필요합니다.")
    @Valid
    private List<MissionUpdateRequest> missions;

    @Builder
    public ChallengeUpdateRequest(String title, String badge, Integer reward, List<MissionUpdateRequest> missions) {
        this.title = title;
        this.badge = badge;
        this.reward = reward;
        this.missions = missions;
    }

    public ChallengeUpdateServiceRequest toServiceRequest() {
        return ChallengeUpdateServiceRequest.builder()
                .title(title)
                .badge(badge)
                .reward(reward)
                .missions(missions.stream()
                        .map(MissionUpdateRequest::toServiceRequest)
                        .collect(Collectors.toList()))
                .build();
    }

    @Getter
    @NoArgsConstructor
    public static class MissionUpdateRequest {

        @NotNull(message = "미션 카테고리는 필수입니다.")
        private MissionCategoryStatus category;

        @NotBlank(message = "미션 제목은 필수입니다.")
        private String title;

        @NotNull(message = "미션 리워드는 필수입니다.")
        private Integer reward;

        @NotBlank(message = "미션 내용은 필수입니다.")
        private String content;

        @Builder
        public MissionUpdateRequest(MissionCategoryStatus category, String title, Integer reward, String content) {
            this.category = category;
            this.title = title;
            this.reward = reward;
            this.content = content;
        }

        public MissionUpdateServiceRequest toServiceRequest() {
            return MissionUpdateServiceRequest.builder()
                    .category(category)
                    .title(title)
                    .reward(reward)
                    .content(content)
                    .build();
        }
    }
}
