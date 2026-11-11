package com.quitmate.user.userPass.entity;

import com.quitmate.global.BaseTimeEntity;
import com.quitmate.user.userPass.enums.UserPassProduct;
import com.quitmate.user.userPass.enums.UserPassSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * QuitMate 90 이용권(KAN-143). 유효 = revoked_at이 없고 starts_at &lt;= 지금 &lt; ends_at.
 *
 * <p>운영 테이블은 KAN-145 DDL로 만든다. 운영 관리자 서버는 ddl-auto: update라, 엔티티가 DDL과 다르면
 * Hibernate가 테이블·컬럼을 직접 만들어 버린다. 그래서 컬럼명·길이·NULL 여부·인덱스 이름을 DDL과 맞추고,
 * user_id는 FK가 생기지 않도록 연관관계 없이 숫자로 둔다. 상품·출처는 MySQL ENUM이 아닌 VARCHAR로 매핑한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_pass",
        indexes = @Index(name = "idx_user_pass_user_ends", columnList = "user_id, ends_at")
)
public class UserPass extends BaseTimeEntity {

    public static final int MEMO_MAX_LENGTH = 255;
    private static final String REVOKE_MEMO_PREFIX = "[회수] ";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private UserPassProduct product;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private UserPassSource source;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(length = MEMO_MAX_LENGTH)
    private String memo;

    @Builder(access = AccessLevel.PRIVATE)
    private UserPass(Long userId, UserPassProduct product, UserPassSource source, LocalDateTime startsAt,
                     LocalDateTime endsAt, LocalDateTime revokedAt, String memo) {
        this.userId = userId;
        this.product = product;
        this.source = source;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.revokedAt = revokedAt;
        this.memo = memo;
    }

    /** 관리자 부여(출처 ADMIN_GRANT) */
    public static UserPass adminGrant(Long userId, UserPassProduct product, LocalDateTime startsAt,
                                      LocalDateTime endsAt, String memo) {
        return UserPass.builder()
                .userId(userId)
                .product(product)
                .source(UserPassSource.ADMIN_GRANT)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .memo(StringUtils.hasText(memo) ? memo.trim() : null)
                .build();
    }

    /** 테스트·다른 출처 데이터 준비용 */
    public static UserPass of(Long userId, UserPassProduct product, UserPassSource source, LocalDateTime startsAt,
                              LocalDateTime endsAt, LocalDateTime revokedAt, String memo) {
        return UserPass.builder()
                .userId(userId)
                .product(product)
                .source(source)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .revokedAt(revokedAt)
                .memo(memo)
                .build();
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isEndedAt(LocalDateTime now) {
        return !endsAt.isAfter(now);
    }

    /** 회수 사유를 기존 메모 뒤에 붙인 결과. 사유가 없으면 기존 메모 그대로 */
    public String memoWithRevokeReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return memo;
        }
        String line = REVOKE_MEMO_PREFIX + reason.trim();
        return StringUtils.hasText(memo) ? memo + "\n" + line : line;
    }

    public void revoke(LocalDateTime revokedAt, String reason) {
        this.memo = memoWithRevokeReason(reason);
        this.revokedAt = revokedAt;
    }
}
