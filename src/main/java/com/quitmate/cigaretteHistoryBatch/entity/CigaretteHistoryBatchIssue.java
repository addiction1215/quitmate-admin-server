package com.quitmate.cigaretteHistoryBatch.entity;

import com.quitmate.cigaretteHistoryBatch.enums.CigaretteHistoryBatchIssueStage;
import com.quitmate.cigaretteHistoryBatch.enums.CigaretteHistoryBatchIssueStatus;
import com.quitmate.global.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 흡연 기록 이관에 실패한 (날짜, 사용자) 한 건. 행은 앱 서버(addiction) 이관 배치가 만들고 갱신한다.
 * 관리자 서버는 PENDING 목록을 읽고 [무시](IGNORED + 메모)만 바꾼다.
 *
 * <p>운영 테이블은 정현님이 설계 문서(KAN-73) 4.6.2 DDL로 만들었다. 운영 관리자 서버는 ddl-auto: update라
 * 상태 컬럼이 MySQL ENUM으로 다시 만들어지지 않게 VARCHAR로 매핑하고, 컬럼명·길이·제약 이름을 DDL과 맞춘다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "cigarette_history_batch_issue",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cigarette_history_batch_issue_date_user",
                columnNames = {"target_date", "user_id"}
        ),
        indexes = @Index(
                name = "idx_cigarette_history_batch_issue_status",
                columnList = "status, target_date"
        )
)
public class CigaretteHistoryBatchIssue extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_date", nullable = false, length = 8)
    private String targetDate;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private CigaretteHistoryBatchIssueStage stage;

    @Column(name = "fail_count", nullable = false)
    private int failCount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private CigaretteHistoryBatchIssueStatus status;

    @Column(name = "first_failed_at", nullable = false)
    private LocalDateTime firstFailedAt;

    @Column(name = "last_failed_at", nullable = false)
    private LocalDateTime lastFailedAt;

    @Column(name = "last_error_type", length = 200)
    private String lastErrorType;

    @Column(name = "last_attempt_id")
    private Long lastAttemptId;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(length = 500)
    private String memo;

    /**
     * 테스트에서 앱 서버가 만든 행을 흉내 낼 때 쓴다. 관리자 서버 코드는 행을 새로 만들지 않는다.
     */
    @Builder
    private CigaretteHistoryBatchIssue(String targetDate, Long userId, CigaretteHistoryBatchIssueStage stage,
                                       int failCount, CigaretteHistoryBatchIssueStatus status,
                                       LocalDateTime firstFailedAt, LocalDateTime lastFailedAt,
                                       String lastErrorType, Long lastAttemptId, String memo) {
        this.targetDate = targetDate;
        this.userId = userId;
        this.stage = stage;
        this.failCount = failCount;
        this.status = status;
        this.firstFailedAt = firstFailedAt;
        this.lastFailedAt = lastFailedAt;
        this.lastErrorType = lastErrorType;
        this.lastAttemptId = lastAttemptId;
        this.memo = memo;
    }
}
