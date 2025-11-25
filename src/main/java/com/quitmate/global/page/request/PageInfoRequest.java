package com.quitmate.global.page.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class PageInfoRequest {
    @Default
    @NotNull(message = "페이지 번호는 필수입니다.")
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    private int page = 1;

    @Default
    @NotNull(message = "페이지 크기는 필수입니다.")
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    private int size = 12;

    public PageInfoServiceRequest toServiceRequest() {
        return PageInfoServiceRequest.builder()
                .page(page)
                .size(size)
                .build();
    }
}
