package com.quitmate.user.userPass.service.request;

import com.quitmate.global.page.request.PageInfoServiceRequest;
import com.quitmate.user.userPass.enums.UserPassStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
public class UserPassListServiceRequest extends PageInfoServiceRequest {

    private final Long userId;
    private final UserPassStatus status;

    @Builder
    public UserPassListServiceRequest(int page, int size, Long userId, UserPassStatus status) {
        super(page, size);
        this.userId = userId;
        this.status = status;
    }
}
