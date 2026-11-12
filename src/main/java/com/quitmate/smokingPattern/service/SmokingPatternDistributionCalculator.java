package com.quitmate.smokingPattern.service;

import com.quitmate.smokingPattern.enums.BaselineComparisonType;
import com.quitmate.smokingPattern.enums.DataSufficiencyLevel;
import com.quitmate.smokingPattern.enums.IndicatorStatus;
import com.quitmate.smokingPattern.enums.RoutineType;
import com.quitmate.smokingPattern.enums.TimeSlot;
import com.quitmate.smokingPattern.enums.TrendDirection;
import com.quitmate.smokingPattern.repository.response.SmokingPatternSnapshotRow;
import com.quitmate.smokingPattern.result.SmokingPatternResult;
import com.quitmate.smokingPattern.service.response.SmokingPatternDistribution;
import com.quitmate.smokingPattern.service.response.SmokingPatternDistribution.CravingResponseSummary;
import com.quitmate.smokingPattern.service.response.SmokingPatternDistribution.HourCounts;
import com.quitmate.smokingPattern.service.response.SmokingPatternDistribution.RelapseMomentsSummary;
import com.quitmate.smokingPattern.service.response.SmokingPatternDistribution.TypeCounts;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 한 주 스냅숏을 한 줄씩 받아 지표 분포를 더한다(DB·시계 없음). 전체·유료·무료마다 하나씩 쓴다.
 *
 * <p>시간대(1)·추세(5)·루틴(6)·충족도(9)는 스냅숏의 스칼라 컬럼으로, 6시간 시간대(1)·기준 비교(4)·갈망 대응(7)·
 * 재흡연 순간(8)은 result_json으로 센다. 지표는 status가 OK일 때만 값으로 세고, 나머지는 계산하지 못한 것으로 센다.
 */
public class SmokingPatternDistributionCalculator {

    private static final int HOURS = 24;
    private static final int MAX_OBSERVED_DAYS = 7;

    private long snapshotCount;
    private long unreadableResultCount;
    private final Map<String, Long> algorithmVersions = new TreeMap<>();
    private final Map<DataSufficiencyLevel, Long> dataSufficiency = zeroCounts(DataSufficiencyLevel.class);
    private final long[] observedDays = new long[MAX_OBSERVED_DAYS + 1];
    private long smokeCountTotal;

    private final long[] peakHours = new long[HOURS];
    private long peakHourNotCalculated;
    private final Map<TimeSlot, Long> peakSlot = zeroCounts(TimeSlot.class);
    private final Map<BaselineComparisonType, Long> baselineComparison = zeroCounts(BaselineComparisonType.class);
    private final Map<TrendDirection, Long> trendDirection = zeroCounts(TrendDirection.class);
    private final Map<RoutineType, Long> routineType = zeroCounts(RoutineType.class);

    private long cravingCalculatedUsers;
    private long cravingNoDataUsers;
    private long cravingSessionCount;
    private long cravingCompletedCount;
    private long cravingSmokedAfterCount;
    private long cravingNotSmokedAfterCount;

    private long relapseCalculatedUsers;
    private long relapseNoDataUsers;
    private long relapseSmokeFreeDays;
    private long relapseCount;
    private long relapsedUsers;
    private final Map<DayOfWeek, Long> relapseByDayOfWeek = zeroCounts(DayOfWeek.class);
    private final long[] relapseByHour = new long[HOURS];
    private long relapseUnknownTimeCount;

    public void add(SmokingPatternSnapshotRow row) {
        snapshotCount++;
        algorithmVersions.merge(String.valueOf(row.algorithmVersion()), 1L, Long::sum);
        if (row.dataSufficiency() != null) {
            dataSufficiency.merge(row.dataSufficiency(), 1L, Long::sum);
        }
        observedDays[Math.max(0, Math.min(MAX_OBSERVED_DAYS, row.observedDays()))]++;
        smokeCountTotal += row.smokeCount();

        if (isHour(row.peakHour())) {
            peakHours[row.peakHour()]++;
        } else {
            peakHourNotCalculated++;
        }
        countIfPresent(trendDirection, row.trendDirection());
        countIfPresent(routineType, row.routineType());

        SmokingPatternResult result = row.result();
        if (result == null) {
            unreadableResultCount++;
            return;
        }
        addPeakSlot(result.peakTime());
        addBaselineComparison(result.baselineComparison());
        addCravingResponse(result.cravingResponse());
        addRelapseMoments(result.relapseMoments());
    }

    public SmokingPatternDistribution result() {
        return new SmokingPatternDistribution(
                snapshotCount,
                unreadableResultCount,
                new LinkedHashMap<>(algorithmVersions),
                new EnumMap<>(dataSufficiency),
                toList(observedDays),
                smokeCountTotal,
                snapshotCount == 0 ? null : Math.round(smokeCountTotal * 10.0 / snapshotCount) / 10.0,
                new HourCounts(toList(peakHours), peakHourNotCalculated),
                typeCounts(peakSlot),
                typeCounts(baselineComparison),
                typeCounts(trendDirection),
                typeCounts(routineType),
                new CravingResponseSummary(cravingCalculatedUsers, cravingNoDataUsers, cravingSessionCount,
                        cravingCompletedCount, cravingSmokedAfterCount, cravingNotSmokedAfterCount,
                        percent(cravingNotSmokedAfterCount, cravingSmokedAfterCount + cravingNotSmokedAfterCount)),
                new RelapseMomentsSummary(relapseCalculatedUsers, relapseNoDataUsers, relapseSmokeFreeDays,
                        relapseCount, relapsedUsers, names(relapseByDayOfWeek), toList(relapseByHour),
                        relapseUnknownTimeCount)
        );
    }

    private void addPeakSlot(SmokingPatternResult.PeakTime peakTime) {
        if (peakTime != null && peakTime.status() == IndicatorStatus.OK) {
            countIfPresent(peakSlot, peakTime.peakSlot());
        }
    }

    private void addBaselineComparison(SmokingPatternResult.BaselineComparison comparison) {
        if (comparison != null && comparison.status() == IndicatorStatus.OK) {
            countIfPresent(baselineComparison, comparison.comparison());
        }
    }

    private void addCravingResponse(SmokingPatternResult.CravingResponse craving) {
        if (craving == null) {
            return;
        }
        if (craving.status() == IndicatorStatus.NO_DATA) {
            cravingNoDataUsers++;
            return;
        }
        if (craving.status() != IndicatorStatus.OK) {
            return;
        }
        cravingCalculatedUsers++;
        cravingSessionCount += craving.sessionCount();
        cravingCompletedCount += craving.completedCount();
        cravingSmokedAfterCount += craving.smokedAfterCount();
        cravingNotSmokedAfterCount += craving.notSmokedAfterCount();
    }

    private void addRelapseMoments(SmokingPatternResult.RelapseMoments relapse) {
        if (relapse == null) {
            return;
        }
        if (relapse.status() == IndicatorStatus.NO_DATA) {
            relapseNoDataUsers++;
            return;
        }
        if (relapse.status() != IndicatorStatus.OK) {
            return;
        }
        relapseCalculatedUsers++;
        relapseSmokeFreeDays += relapse.smokeFreeDays();
        relapseCount += relapse.relapseCount();
        if (relapse.relapseCount() > 0) {
            relapsedUsers++;
        }
        if (relapse.moments() == null) {
            return;
        }
        for (SmokingPatternResult.RelapseMoment moment : relapse.moments()) {
            DayOfWeek dayOfWeek = parseDayOfWeek(moment.dayOfWeek());
            if (dayOfWeek != null) {
                relapseByDayOfWeek.merge(dayOfWeek, 1L, Long::sum);
            }
            Integer hour = parseHour(moment.time());
            if (hour == null) {
                relapseUnknownTimeCount++;
            } else {
                relapseByHour[hour]++;
            }
        }
    }

    /** "HH:mm"의 시. 형식이 다르면 null */
    private static Integer parseHour(String time) {
        if (time == null || time.length() < 2) {
            return null;
        }
        try {
            int hour = Integer.parseInt(time.substring(0, 2));
            return isHour(hour) ? hour : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static DayOfWeek parseDayOfWeek(String dayOfWeek) {
        if (dayOfWeek == null) {
            return null;
        }
        try {
            return DayOfWeek.valueOf(dayOfWeek);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean isHour(Integer hour) {
        return hour != null && hour >= 0 && hour < HOURS;
    }

    private static <E extends Enum<E>> void countIfPresent(Map<E, Long> counts, E value) {
        if (value != null) {
            counts.merge(value, 1L, Long::sum);
        }
    }

    /** 계산하지 못한 수 = 전체 스냅숏 - 값으로 센 수 */
    private <E extends Enum<E>> TypeCounts typeCounts(Map<E, Long> counts) {
        long counted = counts.values().stream().mapToLong(Long::longValue).sum();
        return new TypeCounts(names(counts), snapshotCount - counted);
    }

    private static <E extends Enum<E>> Map<String, Long> names(Map<E, Long> counts) {
        Map<String, Long> named = new LinkedHashMap<>();
        counts.forEach((key, value) -> named.put(key.name(), value));
        return named;
    }

    private static <E extends Enum<E>> Map<E, Long> zeroCounts(Class<E> type) {
        Map<E, Long> counts = new EnumMap<>(type);
        for (E value : type.getEnumConstants()) {
            counts.put(value, 0L);
        }
        return counts;
    }

    private static List<Long> toList(long[] values) {
        return Arrays.stream(values).boxed().toList();
    }

    private static Integer percent(long part, long whole) {
        return whole == 0 ? null : (int) Math.round(part * 100.0 / whole);
    }
}
