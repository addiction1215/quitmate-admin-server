package com.quitmate.smokingPattern;

import com.quitmate.smokingPattern.entity.SmokingPatternResultConverter;
import com.quitmate.smokingPattern.enums.BaselineComparisonType;
import com.quitmate.smokingPattern.enums.DataSufficiencyLevel;
import com.quitmate.smokingPattern.enums.IndicatorStatus;
import com.quitmate.smokingPattern.enums.RoutineType;
import com.quitmate.smokingPattern.enums.TimeSlot;
import com.quitmate.smokingPattern.enums.TrendDirection;
import com.quitmate.smokingPattern.result.SmokingPatternResult;
import com.quitmate.smokingPattern.result.SmokingPatternResult.BaselineComparison;
import com.quitmate.smokingPattern.result.SmokingPatternResult.CravingResponse;
import com.quitmate.smokingPattern.result.SmokingPatternResult.DataSufficiency;
import com.quitmate.smokingPattern.result.SmokingPatternResult.FirstCigarette;
import com.quitmate.smokingPattern.result.SmokingPatternResult.PeakTime;
import com.quitmate.smokingPattern.result.SmokingPatternResult.RelapseMoment;
import com.quitmate.smokingPattern.result.SmokingPatternResult.RelapseMoments;
import com.quitmate.smokingPattern.result.SmokingPatternResult.Routine;
import com.quitmate.smokingPattern.result.SmokingPatternResult.RoutineSlot;
import com.quitmate.smokingPattern.result.SmokingPatternResult.SmokingInterval;
import com.quitmate.smokingPattern.result.SmokingPatternResult.Trend;
import com.quitmate.smokingPattern.repository.response.SmokingPatternSnapshotRow;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 진단 테스트용 데이터. 스냅숏은 앱 서버 배치가 쓰는 테이블이라, 앱 서버가 쓴 것처럼 SQL로 직접 넣는다.
 */
public final class SmokingPatternFixture {

    private static final SmokingPatternResultConverter CONVERTER = new SmokingPatternResultConverter();

    private SmokingPatternFixture() {
    }

    /** 지표를 모두 계산한(OK) 결과 */
    public static SmokingPatternResult okResult(LocalDate periodStart, int peakHour, TrendDirection trend,
                                                RoutineType routine, BaselineComparisonType comparison,
                                                int sessions, int smokedAfter, List<RelapseMoment> moments) {
        List<Integer> hourly = new ArrayList<>(java.util.Collections.nCopies(24, 0));
        hourly.set(peakHour, 5);
        return new SmokingPatternResult(
                "v1",
                periodStart.toString(),
                periodStart.plusDays(6).toString(),
                periodStart.minusDays(7).toString(),
                360,
                14,
                42,
                new PeakTime(IndicatorStatus.OK, 30, hourly, peakHour, List.of(peakHour), TimeSlot.of(peakHour), 40),
                new FirstCigarette(IndicatorStatus.OK, 6, "07:30", "06:50", "09:10"),
                new SmokingInterval(IndicatorStatus.OK, 95, 300, 30, 6, 0),
                new BaselineComparison(IndicatorStatus.OK, 10, 10.0, 6.0, 7, 7, -40, comparison),
                new Trend(IndicatorStatus.OK, 6.0, 8.0, 7, 7, 1, 0, -25, trend),
                new Routine(IndicatorStatus.OK, routine, 65, List.of(new RoutineSlot(8, 10, 6)), 6, 30),
                new CravingResponse(IndicatorStatus.OK, sessions, sessions, smokedAfter, sessions - smokedAfter,
                        sessions == 0 ? null : (int) Math.round((sessions - smokedAfter) * 100.0 / sessions), 60),
                new RelapseMoments(IndicatorStatus.OK, 2, moments.size(), moments),
                new DataSufficiency(DataSufficiencyLevel.SUFFICIENT, 7, 7, 6, 1, 0, 100, 14, 13)
        );
    }

    /** 기록이 부족해 지표를 계산하지 못한 결과 */
    public static SmokingPatternResult insufficientResult(LocalDate periodStart) {
        return new SmokingPatternResult(
                "v1",
                periodStart.toString(),
                periodStart.plusDays(6).toString(),
                periodStart.minusDays(7).toString(),
                360,
                14,
                1,
                new PeakTime(IndicatorStatus.INSUFFICIENT_DATA, 1, java.util.Collections.nCopies(24, 0), null, List.of(), null, null),
                new FirstCigarette(IndicatorStatus.INSUFFICIENT_DATA, 0, null, null, null),
                new SmokingInterval(IndicatorStatus.INSUFFICIENT_DATA, null, null, 0, 0, 0),
                new BaselineComparison(IndicatorStatus.NO_BASELINE, null, null, null, 1, 0, null, null),
                new Trend(IndicatorStatus.INSUFFICIENT_DATA, null, null, 1, 0, 0, 0, null, null),
                new Routine(IndicatorStatus.INSUFFICIENT_DATA, null, null, List.of(), 1, 1),
                new CravingResponse(IndicatorStatus.NO_DATA, 0, 0, 0, 0, null, 60),
                new RelapseMoments(IndicatorStatus.NO_DATA, 0, 0, List.of()),
                new DataSufficiency(DataSufficiencyLevel.INSUFFICIENT, 7, 1, 1, 0, 6, 14, 14, 2)
        );
    }

    public static RelapseMoment relapse(String date, String dayOfWeek, String time) {
        return new RelapseMoment(date, dayOfWeek, time, 2);
    }

    /** 결과의 값으로 스칼라 컬럼을 채워 행을 만든다(앱 서버 SmokingPatternSnapshot.apply와 같은 규칙). */
    public static SmokingPatternSnapshotRow row(long id, long userId, SmokingPatternResult result) {
        return new SmokingPatternSnapshotRow(id, userId, result.algorithmVersion(), result.dataSufficiency().level(),
                result.dataSufficiency().observedDays(), result.smokeCount(), peakHour(result), routineType(result),
                trendDirection(result), result);
    }

    /** smoking_pattern_snapshot에 한 행을 넣고 id를 돌려준다 */
    public static long insertSnapshot(JdbcTemplate jdbcTemplate, long userId, SmokingPatternResult result) {
        return insertSnapshotJson(jdbcTemplate, userId, result, CONVERTER.convertToDatabaseColumn(result));
    }

    /** JSON을 직접 넣는다(깨진 JSON 확인용). 스칼라 컬럼은 result로 채운다 */
    public static long insertSnapshotJson(JdbcTemplate jdbcTemplate, long userId, SmokingPatternResult result,
                                          String json) {
        LocalDateTime now = LocalDateTime.of(2026, 11, 9, 3, 0, 5);
        jdbcTemplate.update("""
                        insert into smoking_pattern_snapshot (user_id, period_start, period_end, algorithm_version,
                            data_sufficiency, observed_days, smoke_count, peak_hour, routine_type, trend_direction,
                            result_json, created_date, updated_date)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                userId, LocalDate.parse(result.periodStart()), LocalDate.parse(result.periodEnd()),
                result.algorithmVersion(), result.dataSufficiency().level().name(),
                result.dataSufficiency().observedDays(), result.smokeCount(), peakHour(result),
                name(routineType(result)), name(trendDirection(result)), json, now, now);
        return jdbcTemplate.queryForObject("select max(id) from smoking_pattern_snapshot", Long.class);
    }

    /**
     * user_pass(KAN-145)는 이용권 관리 PR의 엔티티가 main에 들어오기 전까지 테스트 DB에 없다. 같은 컬럼으로 만들어 둔다.
     */
    public static void createUserPassTableIfMissing(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                create table if not exists user_pass (
                    id bigint generated by default as identity primary key,
                    user_id bigint not null,
                    product varchar(30) not null,
                    source varchar(20) not null,
                    starts_at timestamp(6) not null,
                    ends_at timestamp(6) not null,
                    revoked_at timestamp(6),
                    memo varchar(255),
                    created_date timestamp(6),
                    updated_date timestamp(6)
                )
                """);
    }

    public static void insertPass(JdbcTemplate jdbcTemplate, long userId, LocalDateTime startsAt, LocalDateTime endsAt,
                                  LocalDateTime revokedAt) {
        jdbcTemplate.update("""
                        insert into user_pass (user_id, product, source, starts_at, ends_at, revoked_at)
                        values (?, 'QUITMATE_90', 'ADMIN_GRANT', ?, ?, ?)
                        """,
                userId, startsAt, endsAt, revokedAt);
    }

    public static void deleteAll(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("delete from smoking_pattern_snapshot");
        jdbcTemplate.update("delete from user_pass");
    }

    private static Integer peakHour(SmokingPatternResult result) {
        return result.peakTime().status() == IndicatorStatus.OK ? result.peakTime().peakHour() : null;
    }

    private static RoutineType routineType(SmokingPatternResult result) {
        return result.routine().status() == IndicatorStatus.OK ? result.routine().type() : null;
    }

    private static TrendDirection trendDirection(SmokingPatternResult result) {
        return result.trend().status() == IndicatorStatus.OK ? result.trend().direction() : null;
    }

    private static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
