package com.quitmate.faq.controller.request;

import com.quitmate.faq.enums.FaqCategory;
import com.quitmate.faq.service.request.FaqCreateServiceRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FaqCreateRequest {

    @NotNull(message = "FAQ 카테고리는 필수입니다.")
    private FaqCategory category;

    @NotNull(message = "상단 고정 여부는 필수입니다.")
    private Boolean pinned;

    @NotNull(message = "정렬 순서는 필수입니다.")
    @Min(value = 0, message = "정렬 순서는 0 이상이어야 합니다.")
    private Integer sortOrder;

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    private String description;

    @Builder
    public FaqCreateRequest(FaqCategory category, Boolean pinned, Integer sortOrder, String title, String description) {
        this.category = category;
        this.pinned = pinned;
        this.sortOrder = sortOrder;
        this.title = title;
        this.description = description;
    }

    public FaqCreateServiceRequest toServiceRequest() {
        return FaqCreateServiceRequest.builder()
                .category(category)
                .pinned(pinned)
                .sortOrder(sortOrder)
                .title(title)
                .description(description)
                .build();
    }
}
