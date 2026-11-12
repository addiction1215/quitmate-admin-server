package com.quitmate.user.userPass.repository.response;

import com.quitmate.user.userPass.enums.UserPassProduct;
import com.quitmate.user.userPass.enums.UserPassSource;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserPassDto {
    private final Long id;
    private final Long userId;
    private final String email;
    private final String nickName;
    private final UserPassProduct product;
    private final UserPassSource source;
    private final LocalDateTime startsAt;
    private final LocalDateTime endsAt;
    private final LocalDateTime revokedAt;
    private final String memo;
    private final LocalDateTime createdDate;

    public UserPassDto(Long id, Long userId, String email, String nickName, UserPassProduct product,
                       UserPassSource source, LocalDateTime startsAt, LocalDateTime endsAt,
                       LocalDateTime revokedAt, String memo, LocalDateTime createdDate) {
        this.id = id;
        this.userId = userId;
        this.email = email;
        this.nickName = nickName;
        this.product = product;
        this.source = source;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.revokedAt = revokedAt;
        this.memo = memo;
        this.createdDate = createdDate;
    }
}
