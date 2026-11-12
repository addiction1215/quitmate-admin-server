package com.quitmate.smokingPattern.service.response;

import com.quitmate.smokingPattern.enums.DataSufficiencyLevel;

import java.util.List;
import java.util.Map;

/**
 * 한 주 스냅숏 묶음(전체·유료·무료 중 하나)의 지표 분포. 지표가 계산되지 않은 사용자(기록 부족)는 notCalculated로 센다.
 *
 * @param snapshotCount         스냅숏 수(= 사용자 수)
 * @param unreadableResultCount result_json을 읽지 못한 스냅숏 수. 이 스냅숏은 JSON에서 읽는 지표(시간대 묶음·4·7·8)에서 빠진다
 * @param algorithmVersions     계산 기준 버전별 스냅숏 수
 * @param dataSufficiency       지표 9 데이터 충족도별 스냅숏 수
 * @param observedDays          지난주 상태를 아는 날 수(0~7일)별 스냅숏 수. 8칸, 칸 번호 = 일수
 * @param smokeCountTotal       지난주 흡연 개비 합계
 * @param smokeCountAverage     사용자 한 명당 지난주 흡연 개비 평균(소수 한 자리). 스냅숏이 없으면 null
 * @param peakHour              지표 1 가장 많이 피운 시(0~23시)별 사용자 수
 * @param peakSlot              지표 1 가장 많이 피운 6시간 시간대(DAWN·MORNING·AFTERNOON·EVENING)별 사용자 수
 * @param baselineComparison    지표 4 평소 하루 개비 대비 실제 기록(LESS·SIMILAR·MORE)별 사용자 수
 * @param trendDirection        지표 5 그 전 주 대비 추세(DECREASED·SIMILAR·INCREASED)별 사용자 수
 * @param routineType           지표 6 루틴형·산발형(ROUTINE·SCATTERED)별 사용자 수
 * @param cravingResponse       지표 7 갈망 대응 후 흡연(추정, 어드민 전용) 합계
 * @param relapseMoments        지표 8 재흡연 위험 순간(어드민 전용) 합계
 */
public record SmokingPatternDistribution(
        long snapshotCount,
        long unreadableResultCount,
        Map<String, Long> algorithmVersions,
        Map<DataSufficiencyLevel, Long> dataSufficiency,
        List<Long> observedDays,
        long smokeCountTotal,
        Double smokeCountAverage,
        HourCounts peakHour,
        TypeCounts peakSlot,
        TypeCounts baselineComparison,
        TypeCounts trendDirection,
        TypeCounts routineType,
        CravingResponseSummary cravingResponse,
        RelapseMomentsSummary relapseMoments
) {

    /**
     * @param hours         0~23시 각 시간의 사용자 수(24칸)
     * @param notCalculated 지표를 계산하지 못한 사용자 수
     */
    public record HourCounts(List<Long> hours, long notCalculated) {
    }

    /**
     * @param counts        값별 사용자 수(값을 모두 담고, 없으면 0)
     * @param notCalculated 지표를 계산하지 못한 사용자 수
     */
    public record TypeCounts(Map<String, Long> counts, long notCalculated) {
    }

    /**
     * 지표 7. 지표를 계산한(status OK) 사용자의 갈망 세션을 모두 더한다.
     *
     * @param calculatedUsers       지표를 계산한 사용자 수
     * @param noDataUsers           지난주 갈망 세션이 없는 사용자 수(NO_DATA)
     * @param notSmokedAfterPercent 세션 뒤 정해진 시간 안에 흡연 기록이 없었던 비율(%). 세션이 없으면 null
     */
    public record CravingResponseSummary(long calculatedUsers, long noDataUsers, long sessionCount, long completedCount,
                                         long smokedAfterCount, long notSmokedAfterCount, Integer notSmokedAfterPercent) {
    }

    /**
     * 지표 8. 지표를 계산한(status OK) 사용자의 금연 확정일·다시 피운 순간을 모두 더한다.
     *
     * @param calculatedUsers  지표를 계산한 사용자 수(지난주 금연 확정일이 있음)
     * @param noDataUsers      지난주 금연 확정일이 없는 사용자 수(NO_DATA)
     * @param relapsedUsers    다시 피운 순간이 한 번 이상 있는 사용자 수
     * @param byDayOfWeek      다시 피운 순간의 요일(MONDAY~SUNDAY)별 수
     * @param byHour           다시 피운 날 첫 기록 시각(0~23시)별 수(24칸)
     * @param unknownTimeCount 시각을 모르는 다시 피운 순간 수
     */
    public record RelapseMomentsSummary(long calculatedUsers, long noDataUsers, long smokeFreeDays, long relapseCount,
                                        long relapsedUsers, Map<String, Long> byDayOfWeek, List<Long> byHour,
                                        long unknownTimeCount) {
    }
}
