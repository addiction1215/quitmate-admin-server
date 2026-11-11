package com.quitmate.user.userPass.service.request;

import com.quitmate.user.userPass.enums.UserPassProduct;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserPassGrantServiceRequest {

    private final Long userId;
    private final UserPassProduct product;
    private final LocalDateTime startsAt;
    private final LocalDateTime endsAt;
    private final String memo;

    @Builder
    private UserPassGrantServiceRequest(Long userId, UserPassProduct product, LocalDateTime startsAt,
                                        LocalDateTime endsAt, String memo) {
        this.userId = userId;
        this.product = product == null ? UserPassProduct.QUITMATE_90 : product;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.memo = memo;
    }
}
