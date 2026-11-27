package com.quitmate.appMetrics.ga4.displayname.entity;

import com.quitmate.appMetrics.ga4.displayname.enums.Ga4SourceType;
import com.quitmate.appMetrics.ga4.displayname.enums.Ga4ValueType;
import com.quitmate.global.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 관리자가 GA4 원래 값에 붙인 이름 하나(KAN-228·230). 기본 이름은 코드({@code Ga4DefaultDisplayNames})에 두고,
 * 이 표에는 관리자가 바꾼 이름만 들어간다.
 *
 * <p>운영 표는 정현님이 설계 문서(KAN-228) 4장 DDL로 만든다(KAN-229). 운영 관리자 서버는 ddl-auto: update라
 * 종류 컬럼이 MySQL ENUM으로 다시 만들어지지 않게 VARCHAR로 매핑하고, 컬럼명·길이·제약 이름을 DDL과 맞춘다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "ga4_display_name",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ga4_display_name_type_value",
                columnNames = {"value_type", "raw_value"}
        )
)
public class Ga4DisplayName extends BaseTimeEntity {

    public static final int RAW_VALUE_MAX_LENGTH = 255;
    public static final int DISPLAY_NAME_MAX_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "value_type", nullable = false, length = 20)
    private Ga4ValueType valueType;

    /** GA4에 들어온 원래 값. 정확히 같은 값에만 이름이 붙는다 */
    @Column(name = "raw_value", nullable = false, length = RAW_VALUE_MAX_LENGTH)
    private String rawValue;

    @Column(name = "display_name", nullable = false, length = DISPLAY_NAME_MAX_LENGTH)
    private String displayName;

    /** 앱 화면·웹 페이지 구분. 화면(SCREEN)에만 쓰고 나머지는 null */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "source_type", length = 10)
    private Ga4SourceType sourceType;

    /** 마지막으로 고친 관리자 userId */
    @Column(name = "updated_by")
    private Long updatedBy;

    @Builder
    private Ga4DisplayName(Ga4ValueType valueType, String rawValue, String displayName,
                           Ga4SourceType sourceType, Long updatedBy) {
        this.valueType = valueType;
        this.rawValue = rawValue;
        this.displayName = displayName;
        this.sourceType = sourceType;
        this.updatedBy = updatedBy;
    }

    public void rename(String displayName, Ga4SourceType sourceType, Long updatedBy) {
        this.displayName = displayName;
        this.sourceType = sourceType;
        this.updatedBy = updatedBy;
    }
}
