package com.quitmate.user.userPass.service.request;

import lombok.Builder;
import lombok.Getter;

@Getter
public class UserPassRevokeServiceRequest {

    private final Long passId;
    private final String reason;

    @Builder
    private UserPassRevokeServiceRequest(Long passId, String reason) {
        this.passId = passId;
        this.reason = reason;
    }
}
