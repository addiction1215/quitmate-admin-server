package com.quitmate.locationAccessLog.entity;

import com.quitmate.locationAccessLog.enums.LocationAccessResourceType;
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

import java.time.LocalDateTime;

/**
 * 관리자 위치정보 열람 이력(KAN-208, 법무 검토 KAN-136). 누가(admin_user_id) 언제(accessed_at) 어느 회원(target_user_id)의
 * 무엇(resource_type·resource_id)을 어느 API 로 봤는지만 남긴다. <b>주소·좌표 값은 남기지 않는다.</b>
 * 고치거나 지우지 않고 쌓기만 한다.
 *
 * <p>운영 테이블은 docs/db/KAN-208-admin-location-access-log.sql 로 만든다. 운영 관리자 서버는 ddl-auto: update 라,
 * 엔티티가 DDL 과 다르면 Hibernate 가 다른 모양으로 테이블·인덱스를 만든다. 그래서 칸 이름·타입·길이·NULL 여부·인덱스 이름을
 * DDL 과 맞춘다(AdminLocationAccessLogDdlTest). 회원 id 는 FK 가 생기지 않게 연관관계 없이 숫자로 둔다 — 회원 행이 지워져도
 * 열람 근거는 남아야 하고, 이력 때문에 회원 삭제가 막히면 안 된다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "admin_location_access_log",
        indexes = {
                @Index(name = "idx_admin_location_access_log_target_accessed", columnList = "target_user_id, accessed_at"),
                @Index(name = "idx_admin_location_access_log_accessed", columnList = "accessed_at")
        }
)
public class AdminLocationAccessLog {

    public static final int RESOURCE_TYPE_MAX_LENGTH = 40;
    public static final int API_MAX_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 열람한 관리자 users.id */
    @Column(name = "admin_user_id", nullable = false)
    private Long adminUserId;

    /** 위치정보 주인 회원 users.id */
    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "resource_type", nullable = false, length = RESOURCE_TYPE_MAX_LENGTH)
    private LocationAccessResourceType resourceType;

    /** resource_type 의 행 id. MISSION_HISTORY_ADDRESS 면 mission_history.id */
    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    /** 메서드 + 경로 패턴. 예: "GET /api/v1/admin/mission-history/{id}" */
    @Column(name = "api", nullable = false, length = API_MAX_LENGTH)
    private String api;

    /** 열람 시각(한국 시각) */
    @Column(name = "accessed_at", nullable = false)
    private LocalDateTime accessedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private AdminLocationAccessLog(Long adminUserId, Long targetUserId, LocationAccessResourceType resourceType,
                                   Long resourceId, String api, LocalDateTime accessedAt) {
        this.adminUserId = adminUserId;
        this.targetUserId = targetUserId;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.api = api;
        this.accessedAt = accessedAt;
    }

    public static AdminLocationAccessLog of(Long adminUserId, Long targetUserId, LocationAccessResourceType resourceType,
                                            Long resourceId, String api, LocalDateTime accessedAt) {
        return AdminLocationAccessLog.builder()
                .adminUserId(adminUserId)
                .targetUserId(targetUserId)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .api(api != null && api.length() > API_MAX_LENGTH ? api.substring(0, API_MAX_LENGTH) : api)
                .accessedAt(accessedAt)
                .build();
    }
}
