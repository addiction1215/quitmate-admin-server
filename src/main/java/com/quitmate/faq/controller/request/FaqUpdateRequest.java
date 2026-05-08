package com.quitmate.faq.controller.request;

import com.quitmate.faq.enums.FaqCategory;
import com.quitmate.faq.service.request.FaqUpdateServiceRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FaqUpdateRequest {

    @NotNull(message = "FAQ ID는 필수입니다.")
    private Long id;

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
    public FaqUpdateRequest(Long id, FaqCategory category, Boolean pinned, Integer sortOrder, String title, String description) {
        this.id = id;
        this.category = category;
        this.pinned = pinned;
        this.sortOrder = sortOrder;
        this.title = title;
        this.description = description;
    }

    public FaqUpdateServiceRequest toServiceRequest() {
        return FaqUpdateServiceRequest.builder()
                .id(id)
                .category(category)
                .pinned(pinned)
                .sortOrder(sortOrder)
                .title(title)
                .description(description)
                .build();
    }
}
