package com.quitmate.smokingPattern.repository;

import com.quitmate.smokingPattern.repository.response.PassPeriod;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 진단 화면의 유료·무료 구분에 쓰는 이용권(user_pass, KAN-145) 조회.
 *
 * <p>user_pass 엔티티는 이용권 관리 PR(KAN-147)에 있어 아직 main에 없으므로, 같은 DDL의 컬럼을 SQL로 직접 읽는다.
 * 유효 판정은 이용권 기준과 같다: {@code product = 'QUITMATE_90' AND revoked_at IS NULL}, 기간은 [starts_at, ends_at).
 */
@Repository
@RequiredArgsConstructor
public class UserPassPeriodRepository {

    private static final String PRODUCT = "QUITMATE_90";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /** [from, to)와 겹치는, 회수되지 않은 이용권이 있는 회원 ID */
    public Set<Long> findPaidUserIds(LocalDateTime from, LocalDateTime to) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("product", PRODUCT)
                .addValue("from", from)
                .addValue("to", to);
        return new HashSet<>(jdbcTemplate.queryForList(
                "select distinct user_id from user_pass"
                        + " where product = :product and revoked_at is null and starts_at < :to and ends_at > :from",
                params, Long.class));
    }

    /** 회원별 회수되지 않은 이용권 기간 */
    public Map<Long, List<PassPeriod>> findPassPeriods(Collection<Long> userIds) {
        Map<Long, List<PassPeriod>> periods = new HashMap<>();
        if (userIds.isEmpty()) {
            return periods;
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("product", PRODUCT)
                .addValue("userIds", userIds);
        jdbcTemplate.query(
                "select user_id, starts_at, ends_at from user_pass"
                        + " where product = :product and revoked_at is null and user_id in (:userIds)",
                params,
                rs -> {
                    periods.computeIfAbsent(rs.getLong("user_id"), id -> new ArrayList<>())
                            .add(new PassPeriod(rs.getObject("starts_at", LocalDateTime.class),
                                    rs.getObject("ends_at", LocalDateTime.class)));
                });
        return periods;
    }
}
