package com.quitmate.user.userPass.controller.request;

import com.quitmate.user.userPass.entity.UserPass;
import com.quitmate.user.userPass.enums.UserPassProduct;
import com.quitmate.user.userPass.service.request.UserPassGrantServiceRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class UserPassGrantRequest {

    @NotNull(message = "회원 ID는 필수입니다.")
    private Long userId;

    /** 없으면 QUITMATE_90 */
    private UserPassProduct product;

    @NotNull(message = "시작 시각은 필수입니다.")
    private LocalDateTime startsAt;

    @NotNull(message = "만료 시각은 필수입니다.")
    private LocalDateTime endsAt;

    @Size(max = UserPass.MEMO_MAX_LENGTH, message = "메모는 255자까지 쓸 수 있습니다.")
    private String memo;

    @Builder
    private UserPassGrantRequest(Long userId, UserPassProduct product, LocalDateTime startsAt,
                                 LocalDateTime endsAt, String memo) {
        this.userId = userId;
        this.product = product;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.memo = memo;
    }

    public UserPassGrantServiceRequest toServiceRequest() {
        return UserPassGrantServiceRequest.builder()
                .userId(userId)
                .product(product)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .memo(memo)
                .build();
    }
}
