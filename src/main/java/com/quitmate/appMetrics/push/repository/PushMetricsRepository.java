package com.quitmate.appMetrics.push.repository;

import com.quitmate.appMetrics.push.repository.response.DeliveryDailyCount;
import com.quitmate.appMetrics.push.repository.response.ErrorCodeCount;
import com.quitmate.appMetrics.push.repository.response.OutboxDailyCount;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 「앱 지표」 푸시 섹션(KAN-221)의 읽기 전용 집계. 숫자 정의와 SQL은 {@link PushMetricsSql} 한 곳에 있다.
 *
 * <p>{@code push_outbox}·{@code push_delivery}는 앱 서버(addiction)가 주인인 표다. 관리자 서버는 운영에서
 * {@code ddl-auto: update}라 엔티티를 만들면 배포 때 Hibernate가 운영 표를 만들거나 칸을 더할 수 있어,
 * 엔티티 없이 SELECT만 한다. {@code push}는 기존 {@code Push} 엔티티의 표다.
 */
@Repository
@RequiredArgsConstructor
public class PushMetricsRepository {

    /** 오류 코드 목록 최대 길이 */
    public static final int ERROR_CODE_LIMIT = PushMetricsSql.ERROR_CODE_LIMIT;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /** 날짜별 push_outbox 집계(사람 단위). 행이 없는 날은 빠진다 */
    public List<OutboxDailyCount> findOutboxDailyCounts(LocalDateTime from, LocalDateTime to) {
        return jdbcTemplate.query(PushMetricsSql.OUTBOX_DAILY, range(from, to), (rs, rowNum) -> new OutboxDailyCount(
                rs.getObject("metric_date", LocalDate.class),
                rs.getLong("requested"),
                rs.getLong("daily_smoking_sent"),
                rs.getLong("daily_smoking_opened")));
    }

    /** 날짜별 push_delivery 집계(기기 단위). 행이 없는 날은 빠진다 */
    public List<DeliveryDailyCount> findDeliveryDailyCounts(LocalDateTime from, LocalDateTime to) {
        return jdbcTemplate.query(PushMetricsSql.DELIVERY_DAILY, range(from, to), (rs, rowNum) -> new DeliveryDailyCount(
                rs.getObject("metric_date", LocalDate.class),
                rs.getLong("total"),
                rs.getLong("ticket_ok"),
                rs.getLong("ticket_error"),
                rs.getLong("receipt_ok"),
                rs.getLong("receipt_error"),
                rs.getLong("receipt_pending")));
    }

    /** ticket_error·receipt_error를 합친 오류 코드별 건수(기기 단위, 많은 순, 최대 {@value #ERROR_CODE_LIMIT}개) */
    public List<ErrorCodeCount> findErrorCodeCounts(LocalDateTime from, LocalDateTime to) {
        return jdbcTemplate.query(PushMetricsSql.ERROR_CODES, range(from, to), (rs, rowNum) -> new ErrorCodeCount(
                rs.getString("error_code"),
                rs.getLong("error_count")));
    }

    /** 푸시 토큰이 있는 회원 수(기간 무관, 지금 값) */
    public long countTokenUsers() {
        Long count = jdbcTemplate.queryForObject(PushMetricsSql.TOKEN_USERS, new MapSqlParameterSource(), Long.class);
        return count == null ? 0 : count;
    }

    private static MapSqlParameterSource range(LocalDateTime from, LocalDateTime to) {
        return new MapSqlParameterSource()
                .addValue("from", from)
                .addValue("to", to);
    }
}
