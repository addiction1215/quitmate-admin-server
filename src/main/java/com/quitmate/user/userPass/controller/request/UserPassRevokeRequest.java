package com.quitmate.user.userPass.controller.request;

import com.quitmate.user.userPass.entity.UserPass;
import com.quitmate.user.userPass.service.request.UserPassRevokeServiceRequest;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserPassRevokeRequest {

    /** 회수 사유(선택). 부여 메모 뒤에 "[회수] 사유"로 붙는다. */
    @Size(max = UserPass.MEMO_MAX_LENGTH, message = "회수 사유는 255자까지 쓸 수 있습니다.")
    private String reason;

    @Builder
    private UserPassRevokeRequest(String reason) {
        this.reason = reason;
    }

    public UserPassRevokeServiceRequest toServiceRequest(Long passId) {
        return UserPassRevokeServiceRequest.builder()
                .passId(passId)
                .reason(reason)
                .build();
    }
}
