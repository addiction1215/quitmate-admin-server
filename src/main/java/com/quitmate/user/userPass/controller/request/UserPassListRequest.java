package com.quitmate.user.userPass.controller.request;

import com.quitmate.global.page.request.PageInfoRequest;
import com.quitmate.user.userPass.enums.UserPassStatus;
import com.quitmate.user.userPass.service.request.UserPassListServiceRequest;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserPassListRequest extends PageInfoRequest {

    /** 이 회원의 이용권만. 없으면 전체 */
    private Long userId;
    /** 조회 시각 기준 상태. 없으면 전체 */
    private UserPassStatus status;

    @Builder
    public UserPassListRequest(int page, int size, Long userId, UserPassStatus status) {
        super(page, size);
        this.userId = userId;
        this.status = status;
    }

    public UserPassListServiceRequest toServiceRequest() {
        return UserPassListServiceRequest.builder()
                .page(getPage())
                .size(getSize())
                .userId(userId)
                .status(status)
                .build();
    }
}
