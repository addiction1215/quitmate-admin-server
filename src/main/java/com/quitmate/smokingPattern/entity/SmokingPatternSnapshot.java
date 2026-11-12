package com.quitmate.smokingPattern.entity;

import com.quitmate.global.BaseTimeEntity;
import com.quitmate.smokingPattern.enums.DataSufficiencyLevel;
import com.quitmate.smokingPattern.enums.RoutineType;
import com.quitmate.smokingPattern.enums.TrendDirection;
import com.quitmate.smokingPattern.result.SmokingPatternResult;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

/**
 * 사용자 한 명의 주간 흡연 패턴 진단 스냅숏(앱 서버 KAN-134가 만든다). 사용자·주차마다 한 행이다.
 *
 * <p>테이블·컬럼은 앱 서버 {@code SmokingPatternSnapshot}(addiction#94)·KAN-133 DDL과 같다. 관리자 서버는 읽기만 하므로
 * {@link Immutable}로 두어 실수로 고쳐 쓰지 않게 한다.
 *
 * <p>운영 DB에는 배포 전에 KAN-133 DDL로 직접 만든다. 운영 {@code ddl-auto: update}가 먼저 만들더라도 같은 모양이 되도록
 * enum은 VARCHAR로 매핑하고 user_id는 FK 없이 값으로만 둔다.
 */
@Entity
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "smoking_pattern_snapshot",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_smoking_pattern_snapshot_user_period",
                columnNames = {"user_id", "period_start"}
        ),
        indexes = @Index(
                name = "idx_smoking_pattern_snapshot_period",
                columnList = "period_start, data_sufficiency"
        )
)
public class SmokingPatternSnapshot extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 분석 주의 월요일 */
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    /** 분석 주의 일요일 */
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "algorithm_version", nullable = false, length = 20)
    private String algorithmVersion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "data_sufficiency", nullable = false, length = 20)
    private DataSufficiencyLevel dataSufficiency;

    /** 지난주 중 상태를 아는 날 수(0~7) */
    @Column(name = "observed_days", nullable = false)
    private int observedDays;

    /** 지난주 흡연 개비 합계 */
    @Column(name = "smoke_count", nullable = false)
    private int smokeCount;

    /** 지표 1의 가장 많이 피운 시(0~23). 기록이 부족하면 null */
    @Column(name = "peak_hour")
    private Integer peakHour;

    /** 지표 6. 기록이 부족하면 null */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "routine_type", length = 20)
    private RoutineType routineType;

    /** 지표 5. 기록이 부족하면 null */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "trend_direction", length = 20)
    private TrendDirection trendDirection;

    /** 지표 1~9 전체. 읽지 못한 JSON이면 null */
    @Convert(converter = SmokingPatternResultConverter.class)
    @Column(name = "result_json", nullable = false, columnDefinition = "TEXT")
    private SmokingPatternResult result;
}
