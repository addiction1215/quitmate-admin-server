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
 * 「앱 지표」 푸시 섹션(KAN-221)의 읽기 전용 집계.
 *
 * <p>{@code push_outbox}·{@code push_delivery}는 앱 서버(addiction)가 주인인 표다. 관리자 서버는 운영에서
 * {@code ddl-auto: update}라 엔티티를 만들면 배포 때 Hibernate가 운영 표를 만들거나 칸을 더할 수 있어,
 * 엔티티 없이 SELECT만 한다. {@code push}는 기존 {@code Push} 엔티티의 표다.
 *
 * <p>시각 칸(created_date)은 앱 서버가 한국 시각(LocalDateTime)으로 넣는다. 그래서 한국 날짜 [from, to)로 그대로 비교한다.
 * SQL은 운영(MySQL)과 테스트(H2, PostgreSQL 모드) 둘 다에서 도는 문법만 쓴다.
 */
@Repository
@RequiredArgsConstructor
public class PushMetricsRepository {

    /** 오류 코드 목록 최대 길이 */
    public static final int ERROR_CODE_LIMIT = 20;

    private static final String OUTBOX_DAILY = """
            select cast(created_date as date) as metric_date,
                   count(*) as requested,
                   sum(case when schedule_id is not null and (status = 'SENT' or opened_at is not null)
                            then 1 else 0 end) as daily_smoking_sent,
                   sum(case when schedule_id is not null and opened_at is not null
                            then 1 else 0 end) as daily_smoking_opened
            from push_outbox
            where created_date >= :from and created_date < :to
            group by cast(created_date as date)
            """;

    private static final String DELIVERY_DAILY = """
            select cast(created_date as date) as metric_date,
                   count(*) as total,
                   sum(case when push_outbox_id is null then 1 else 0 end) as without_outbox,
                   sum(case when ticket_status = 'OK' then 1 else 0 end) as ticket_ok,
                   sum(case when ticket_status = 'ERROR' then 1 else 0 end) as ticket_error,
                   sum(case when receipt_status = 'OK' then 1 else 0 end) as receipt_ok,
                   sum(case when receipt_status = 'ERROR' then 1 else 0 end) as receipt_error,
                   sum(case when receipt_status = 'PENDING' then 1 else 0 end) as receipt_pending
            from push_delivery
            where created_date >= :from and created_date < :to
            group by cast(created_date as date)
            """;

    private static final String ERROR_CODES = """
            select error_code, count(*) as error_count
            from (
                select ticket_error as error_code from push_delivery
                where created_date >= :from and created_date < :to
                  and ticket_error is not null and ticket_error <> ''
                union all
                select receipt_error as error_code from push_delivery
                where created_date >= :from and created_date < :to
                  and receipt_error is not null and receipt_error <> ''
            ) errors_in_period
            group by error_code
            order by error_count desc, error_code asc
            limit\s""" + ERROR_CODE_LIMIT;

    private static final String TOKEN_USERS = """
            select count(distinct user_id)
            from push
            where user_id is not null and push_token is not null and push_token <> ''
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /** 날짜별 push_outbox 집계. 행이 없는 날은 빠진다 */
    public List<OutboxDailyCount> findOutboxDailyCounts(LocalDateTime from, LocalDateTime to) {
        return jdbcTemplate.query(OUTBOX_DAILY, range(from, to), (rs, rowNum) -> new OutboxDailyCount(
                rs.getObject("metric_date", LocalDate.class),
                rs.getLong("requested"),
                rs.getLong("daily_smoking_sent"),
                rs.getLong("daily_smoking_opened")));
    }

    /** 날짜별 push_delivery 집계. 행이 없는 날은 빠진다 */
    public List<DeliveryDailyCount> findDeliveryDailyCounts(LocalDateTime from, LocalDateTime to) {
        return jdbcTemplate.query(DELIVERY_DAILY, range(from, to), (rs, rowNum) -> new DeliveryDailyCount(
                rs.getObject("metric_date", LocalDate.class),
                rs.getLong("total"),
                rs.getLong("without_outbox"),
                rs.getLong("ticket_ok"),
                rs.getLong("ticket_error"),
                rs.getLong("receipt_ok"),
                rs.getLong("receipt_error"),
                rs.getLong("receipt_pending")));
    }

    /** ticket_error·receipt_error를 합친 오류 코드별 건수(많은 순, 최대 {@value #ERROR_CODE_LIMIT}개) */
    public List<ErrorCodeCount> findErrorCodeCounts(LocalDateTime from, LocalDateTime to) {
        return jdbcTemplate.query(ERROR_CODES, range(from, to), (rs, rowNum) -> new ErrorCodeCount(
                rs.getString("error_code"),
                rs.getLong("error_count")));
    }

    /** 푸시 토큰이 있는 회원 수(기간 무관, 지금 값) */
    public long countTokenUsers() {
        Long count = jdbcTemplate.queryForObject(TOKEN_USERS, new MapSqlParameterSource(), Long.class);
        return count == null ? 0 : count;
    }

    private static MapSqlParameterSource range(LocalDateTime from, LocalDateTime to) {
        return new MapSqlParameterSource()
                .addValue("from", from)
                .addValue("to", to);
    }
}
