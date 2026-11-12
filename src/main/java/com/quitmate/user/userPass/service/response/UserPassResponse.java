package com.quitmate.user.userPass.service.response;

import com.quitmate.user.userPass.entity.UserPass;
import com.quitmate.user.userPass.enums.UserPassProduct;
import com.quitmate.user.userPass.enums.UserPassSource;
import com.quitmate.user.userPass.enums.UserPassStatus;
import com.quitmate.user.userPass.repository.response.UserPassDto;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class UserPassResponse {

    private Long id;
    private Long userId;
    private String email;
    private String nickName;
    private UserPassProduct product;
    private UserPassSource source;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private LocalDateTime revokedAt;
    /** 조회 시각 기준 상태: ACTIVE / SCHEDULED / EXPIRED / REVOKED */
    private UserPassStatus status;
    private String memo;
    private LocalDateTime createdDate;

    @Builder
    private UserPassResponse(Long id, Long userId, String email, String nickName, UserPassProduct product,
                             UserPassSource source, LocalDateTime startsAt, LocalDateTime endsAt,
                             LocalDateTime revokedAt, UserPassStatus status, String memo,
                             LocalDateTime createdDate) {
        this.id = id;
        this.userId = userId;
        this.email = email;
        this.nickName = nickName;
        this.product = product;
        this.source = source;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.revokedAt = revokedAt;
        this.status = status;
        this.memo = memo;
        this.createdDate = createdDate;
    }

    public static UserPassResponse of(UserPassDto dto, LocalDateTime now) {
        return UserPassResponse.builder()
                .id(dto.getId())
                .userId(dto.getUserId())
                .email(dto.getEmail())
                .nickName(dto.getNickName())
                .product(dto.getProduct())
                .source(dto.getSource())
                .startsAt(dto.getStartsAt())
                .endsAt(dto.getEndsAt())
                .revokedAt(dto.getRevokedAt())
                .status(UserPassStatus.of(dto.getStartsAt(), dto.getEndsAt(), dto.getRevokedAt(), now))
                .memo(dto.getMemo())
                .createdDate(dto.getCreatedDate())
                .build();
    }

    public static UserPassResponse of(UserPass pass, String email, String nickName, LocalDateTime now) {
        return UserPassResponse.builder()
                .id(pass.getId())
                .userId(pass.getUserId())
                .email(email)
                .nickName(nickName)
                .product(pass.getProduct())
                .source(pass.getSource())
                .startsAt(pass.getStartsAt())
                .endsAt(pass.getEndsAt())
                .revokedAt(pass.getRevokedAt())
                .status(UserPassStatus.of(pass.getStartsAt(), pass.getEndsAt(), pass.getRevokedAt(), now))
                .memo(pass.getMemo())
                .createdDate(pass.getCreatedDate())
                .build();
    }
}
