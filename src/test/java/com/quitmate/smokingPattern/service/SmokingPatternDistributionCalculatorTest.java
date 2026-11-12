package com.quitmate.smokingPattern.service;

import com.quitmate.smokingPattern.enums.BaselineComparisonType;
import com.quitmate.smokingPattern.enums.DataSufficiencyLevel;
import com.quitmate.smokingPattern.enums.RoutineType;
import com.quitmate.smokingPattern.enums.TrendDirection;
import com.quitmate.smokingPattern.repository.response.SmokingPatternSnapshotRow;
import com.quitmate.smokingPattern.service.response.SmokingPatternDistribution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.quitmate.smokingPattern.SmokingPatternFixture.insufficientResult;
import static com.quitmate.smokingPattern.SmokingPatternFixture.okResult;
import static com.quitmate.smokingPattern.SmokingPatternFixture.relapse;
import static com.quitmate.smokingPattern.SmokingPatternFixture.row;
import static org.assertj.core.api.Assertions.assertThat;

class SmokingPatternDistributionCalculatorTest {

    private static final LocalDate WEEK = LocalDate.of(2026, 11, 2);

    @DisplayName("스냅숏이 없으면 모든 수가 0이고 평균은 null이다")
    @Test
    void empty() {
        SmokingPatternDistribution result = new SmokingPatternDistributionCalculator().result();

        assertThat(result.snapshotCount()).isZero();
        assertThat(result.smokeCountAverage()).isNull();
        assertThat(result.observedDays()).hasSize(8).containsOnly(0L);
        assertThat(result.peakHour().hours()).hasSize(24).containsOnly(0L);
        assertThat(result.dataSufficiency()).containsOnlyKeys(DataSufficiencyLevel.values()).containsValues(0L);
        assertThat(result.routineType().counts()).containsEntry("ROUTINE", 0L).containsEntry("SCATTERED", 0L);
        assertThat(result.cravingResponse().notSmokedAfterPercent()).isNull();
        assertThat(result.relapseMoments().byDayOfWeek()).hasSize(7).containsValues(0L);
        assertThat(result.relapseMoments().byHour()).hasSize(24);
    }

    @DisplayName("지표 1~9를 OK일 때만 값으로 세고, 계산하지 못한 사용자는 따로 센다")
    @Test
    void countsOnlyCalculatedIndicators() {
        // given
        SmokingPatternDistributionCalculator calculator = new SmokingPatternDistributionCalculator();
        calculator.add(row(1, 10, okResult(WEEK, 8, TrendDirection.DECREASED, RoutineType.ROUTINE,
                BaselineComparisonType.LESS, 4, 1,
                List.of(relapse("2026-11-04", "WEDNESDAY", "08:15"), relapse("2026-11-07", "SATURDAY", null)))));
        calculator.add(row(2, 11, okResult(WEEK, 21, TrendDirection.INCREASED, RoutineType.SCATTERED,
                BaselineComparisonType.MORE, 2, 2, List.of(relapse("2026-11-05", "THURSDAY", "21:40")))));
        calculator.add(row(3, 12, insufficientResult(WEEK)));

        // when
        SmokingPatternDistribution result = calculator.result();

        // then: 9 충족도·관찰일·개비
        assertThat(result.snapshotCount()).isEqualTo(3);
        assertThat(result.algorithmVersions()).containsExactly(org.assertj.core.api.Assertions.entry("v1", 3L));
        assertThat(result.dataSufficiency())
                .containsEntry(DataSufficiencyLevel.SUFFICIENT, 2L)
                .containsEntry(DataSufficiencyLevel.PARTIAL, 0L)
                .containsEntry(DataSufficiencyLevel.INSUFFICIENT, 1L);
        assertThat(result.observedDays().get(7)).isEqualTo(2);
        assertThat(result.observedDays().get(1)).isEqualTo(1);
        assertThat(result.smokeCountTotal()).isEqualTo(85);
        assertThat(result.smokeCountAverage()).isEqualTo(28.3);

        // 1 시간대
        assertThat(result.peakHour().hours().get(8)).isEqualTo(1);
        assertThat(result.peakHour().hours().get(21)).isEqualTo(1);
        assertThat(result.peakHour().notCalculated()).isEqualTo(1);
        assertThat(result.peakSlot().counts())
                .containsEntry("MORNING", 1L).containsEntry("EVENING", 1L).containsEntry("DAWN", 0L);
        assertThat(result.peakSlot().notCalculated()).isEqualTo(1);

        // 4·5·6
        assertThat(result.baselineComparison().counts()).containsEntry("LESS", 1L).containsEntry("MORE", 1L);
        assertThat(result.baselineComparison().notCalculated()).isEqualTo(1);
        assertThat(result.trendDirection().counts()).containsEntry("DECREASED", 1L).containsEntry("INCREASED", 1L);
        assertThat(result.routineType().counts()).containsEntry("ROUTINE", 1L).containsEntry("SCATTERED", 1L);
        assertThat(result.routineType().notCalculated()).isEqualTo(1);

        // 7 갈망 대응(세션 6, 뒤에 피움 3, 안 피움 3)
        assertThat(result.cravingResponse().calculatedUsers()).isEqualTo(2);
        assertThat(result.cravingResponse().noDataUsers()).isEqualTo(1);
        assertThat(result.cravingResponse().sessionCount()).isEqualTo(6);
        assertThat(result.cravingResponse().smokedAfterCount()).isEqualTo(3);
        assertThat(result.cravingResponse().notSmokedAfterCount()).isEqualTo(3);
        assertThat(result.cravingResponse().notSmokedAfterPercent()).isEqualTo(50);

        // 8 재흡연 순간(시각 모르는 1건 포함)
        assertThat(result.relapseMoments().calculatedUsers()).isEqualTo(2);
        assertThat(result.relapseMoments().noDataUsers()).isEqualTo(1);
        assertThat(result.relapseMoments().relapseCount()).isEqualTo(3);
        assertThat(result.relapseMoments().relapsedUsers()).isEqualTo(2);
        assertThat(result.relapseMoments().smokeFreeDays()).isEqualTo(4);
        assertThat(result.relapseMoments().byDayOfWeek())
                .containsEntry("WEDNESDAY", 1L).containsEntry("THURSDAY", 1L).containsEntry("SATURDAY", 1L)
                .containsEntry("MONDAY", 0L);
        assertThat(result.relapseMoments().byHour().get(8)).isEqualTo(1);
        assertThat(result.relapseMoments().byHour().get(21)).isEqualTo(1);
        assertThat(result.relapseMoments().unknownTimeCount()).isEqualTo(1);
    }

    @DisplayName("result_json을 읽지 못한 스냅숏은 스칼라 컬럼으로만 세고, JSON 지표에서는 계산하지 못한 것으로 센다")
    @Test
    void unreadableResult() {
        // given
        SmokingPatternSnapshotRow readable = row(1, 10, okResult(WEEK, 8, TrendDirection.SIMILAR,
                RoutineType.ROUTINE, BaselineComparisonType.SIMILAR, 1, 0, List.of()));
        SmokingPatternSnapshotRow unreadable = new SmokingPatternSnapshotRow(2L, 11L, "v1",
                DataSufficiencyLevel.PARTIAL, 4, 9, 22, RoutineType.SCATTERED, TrendDirection.DECREASED, null);
        SmokingPatternDistributionCalculator calculator = new SmokingPatternDistributionCalculator();

        // when
        calculator.add(readable);
        calculator.add(unreadable);
        SmokingPatternDistribution result = calculator.result();

        // then
        assertThat(result.unreadableResultCount()).isEqualTo(1);
        assertThat(result.peakHour().hours().get(22)).isEqualTo(1);
        assertThat(result.routineType().counts()).containsEntry("SCATTERED", 1L);
        assertThat(result.peakSlot().notCalculated()).isEqualTo(1);
        assertThat(result.baselineComparison().notCalculated()).isEqualTo(1);
        assertThat(result.cravingResponse().calculatedUsers()).isEqualTo(1);
        assertThat(result.cravingResponse().notSmokedAfterPercent()).isEqualTo(100);
    }
}
