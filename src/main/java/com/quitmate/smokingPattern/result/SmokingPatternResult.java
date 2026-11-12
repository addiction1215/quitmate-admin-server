package com.quitmate.smokingPattern.result;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.quitmate.smokingPattern.enums.BaselineComparisonType;
import com.quitmate.smokingPattern.enums.DataSufficiencyLevel;
import com.quitmate.smokingPattern.enums.IndicatorStatus;
import com.quitmate.smokingPattern.enums.RoutineType;
import com.quitmate.smokingPattern.enums.TimeSlot;
import com.quitmate.smokingPattern.enums.TrendDirection;

/**
 * 한 사용자의 주간 흡연 패턴 진단 결과. 스냅숏 테이블의 result_json 컬럼에 그대로 저장한다.
 *
 * <p><b>앱 서버 {@code com.addiction.smokingPattern.analyzer.SmokingPatternResult}(addiction#94, algorithmVersion v1)와 같은 모양이다.</b>
 * 앱 서버 배치가 쓰고 관리자 서버는 읽기만 한다. 앱 서버에서 필드를 바꾸면 여기도 맞춘다(모르는 필드·enum 값은 무시하거나 null로 읽는다).
 *
 * <p>날짜는 "yyyy-MM-dd", 시각은 "HH:mm" 문자열로 둔다. 어드민 서버처럼 다른 곳에서 JSON을 읽을 때 날짜 직렬화 설정에
 * 따라 모양이 달라지지 않게 하기 위해서다. 알고리즘을 바꾸면서 필드를 빼더라도 예전 스냅숏을 읽을 수 있도록 모르는 필드는 무시한다.
 *
 * <p>진단 문구는 의학적 진단이나 성공 확률처럼 읽히지 않게, 기록에서 보이는 경향으로만 설명한다.
 *
 * @param algorithmVersion 계산 기준 버전. 기준값이나 계산 방식을 바꾸면 올린다
 * @param periodStart      분석 주의 월요일
 * @param periodEnd        분석 주의 일요일
 * @param sampleStart      시간대·루틴 지표(1·2·6)에 쓴 표본 기간의 첫날. 기본은 periodEnd 포함 최근 14일
 * @param sleepGapMinutes  이 시간(분) 이상이고 새벽(00~06시)에 걸친 흡연 간격은 수면으로 보고 간격 지표에서 뺐다
 * @param sampleDays       표본 기간 일수
 * @param smokeCount       지난주(가입 뒤 날짜) 흡연 개비 합계
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SmokingPatternResult(
	String algorithmVersion,
	String periodStart,
	String periodEnd,
	String sampleStart,
	int sleepGapMinutes,
	int sampleDays,
	int smokeCount,
	PeakTime peakTime,
	FirstCigarette firstCigarette,
	SmokingInterval smokingInterval,
	BaselineComparison baselineComparison,
	Trend trend,
	Routine routine,
	CravingResponse cravingResponse,
	RelapseMoments relapseMoments,
	DataSufficiency dataSufficiency
) {

	/**
	 * 지표 1. 주요 흡연 시간대(표본 기간).
	 *
	 * @param sampleSmokeCount 표본 기간에 시각을 아는 흡연 기록 수
	 * @param hourlyCounts     0~23시 각 시간의 흡연 기록 수(24칸)
	 * @param peakHour         가장 많이 피운 시(0~23). 같으면 이른 시각. 기록이 없으면 null
	 * @param topHours         많이 피운 시 최대 3개(많은 순)
	 * @param peakSlot         가장 많이 피운 6시간 단위 시간대
	 * @param peakSlotPercent  표본 흡연 중 peakSlot 비율(%)
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record PeakTime(IndicatorStatus status, int sampleSmokeCount, List<Integer> hourlyCounts, Integer peakHour,
						   List<Integer> topHours, TimeSlot peakSlot, Integer peakSlotPercent) {
	}

	/**
	 * 지표 2. 하루 첫 담배 시각(지난주). 수면으로 본 간격 바로 뒤의 첫 기록을 그날의 첫 담배로 본다.
	 * 새벽 1시에 피우고 잔 뒤 아침 7시에 피웠다면 그날의 첫 담배는 7시다.
	 *
	 * @param daysCounted  첫 담배를 찾은 날 수
	 * @param typicalTime  보통 첫 담배 시각(중앙값, "HH:mm")
	 * @param earliestTime 가장 이른 첫 담배 시각
	 * @param latestTime   가장 늦은 첫 담배 시각
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record FirstCigarette(IndicatorStatus status, int daysCounted, String typicalTime, String earliestTime,
								 String latestTime) {
	}

	/**
	 * 지표 3. 수면을 뺀 흡연 간격과 최장 참은 시간(지난주). 지난주에 피운 기록과 그 직전 기록 사이의 간격을 본다.
	 *
	 * @param averageIntervalMinutes 깨어 있는 동안의 평균 흡연 간격(분)
	 * @param longestHoldMinutes     깨어 있는 동안 가장 오래 참은 간격(분)
	 * @param awakeGapCount          평균에 쓴 간격 수
	 * @param sleepGapCount          수면으로 보고 뺀 간격 수
	 * @param excludedLongGapCount   24시간 이상이라 기록이 끊긴 것으로 보고 뺀 간격 수
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record SmokingInterval(IndicatorStatus status, Integer averageIntervalMinutes, Integer longestHoldMinutes,
								  int awakeGapCount, int sleepGapCount, int excludedLongGapCount) {
	}

	/**
	 * 지표 4. 자기보고 기준(평소 하루 개비)과 실제 기록 비교(지난주, 상태를 아는 날만).
	 *
	 * @param baselineDailyCount   지난주 일요일에 적용된 평소 하루 개비
	 * @param averageBaselineCount 비교한 날들의 평소 하루 개비 평균. 주 중에 기준값이 바뀌었으면 날짜별 값을 쓴다
	 * @param averageDailyCount    비교한 날들의 실제 하루 평균 개비(금연 확정일은 0)
	 * @param knownDays            상태를 아는 날 수
	 * @param comparedDays         그중 기준값이 있어 비교한 날 수
	 * @param differencePercent    (실제 - 기준) / 기준 × 100, 반올림
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record BaselineComparison(IndicatorStatus status, Integer baselineDailyCount, Double averageBaselineCount,
									 Double averageDailyCount, int knownDays, int comparedDays,
									 Integer differencePercent, BaselineComparisonType comparison) {
	}

	/**
	 * 지표 5. 추세. 지난주(current)와 그 전 주(previous)의 하루 평균 개비를 상태를 아는 날로만 비교한다.
	 *
	 * @param changePercent 그 전 주 대비 변화율(%). 그 전 주 평균이 0이면 null
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Trend(IndicatorStatus status, Double currentAverage, Double previousAverage, int currentKnownDays,
						int previousKnownDays, int currentSmokeFreeDays, int previousSmokeFreeDays,
						Integer changePercent, TrendDirection direction) {
	}

	/**
	 * 지표 6. 루틴형·산발형(표본 기간). 2시간 단위 칸 중 여러 날 반복해서 피운 칸 3개에 흡연이 얼마나 몰리는지 본다.
	 *
	 * @param routineSharePercent 표본 흡연 중 routineSlots에 든 비율(%)
	 * @param routineSlots        여러 날 반복해서 피운 2시간 칸(최대 3개)
	 * @param smokingDays         표본 기간 중 시각을 아는 흡연 기록이 있는 날 수
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Routine(IndicatorStatus status, RoutineType type, Integer routineSharePercent,
						  List<RoutineSlot> routineSlots, int smokingDays, int sampleSmokeCount) {
	}

	/**
	 * @param startHour 칸 시작 시(0, 2, ..., 22)
	 * @param endHour   칸 끝 시(2, 4, ..., 24)
	 * @param days      이 칸에 피운 날 수
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record RoutineSlot(int startHour, int endHour, int days) {
	}

	/**
	 * 지표 7. 갈망 대응 후 흡연 여부(추정, 어드민 전용). 지난주에 시작한 갈망 세션마다 시작부터 끝(완료하지 않았으면 시작)
	 * 후 windowMinutes분 안에 흡연 기록이 있는지 본다. 기록하지 않고 피웠을 수 있으므로 추정이다.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record CravingResponse(IndicatorStatus status, int sessionCount, int completedCount,
								  int smokedAfterCount, int notSmokedAfterCount, Integer notSmokedAfterPercent,
								  int windowMinutes) {
	}

	/**
	 * 지표 8. 재흡연 위험 순간(어드민 전용). 금연 확정일 다음 날 다시 피운 순간을 모은다.
	 *
	 * @param smokeFreeDays 지난주 금연 확정일 수(그날 흡연 기록 없음)
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record RelapseMoments(IndicatorStatus status, int smokeFreeDays, int relapseCount,
								 List<RelapseMoment> moments) {
	}

	/**
	 * @param date                다시 피운 날("yyyy-MM-dd")
	 * @param dayOfWeek           요일(MONDAY ~ SUNDAY)
	 * @param time                그날 첫 기록 시각("HH:mm"). 시각을 모르면 null
	 * @param smokeFreeStreakDays 다시 피우기 전까지 이어진 금연 확정 일수(분석 기간 안에서만 셈)
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record RelapseMoment(String date, String dayOfWeek, String time, int smokeFreeStreakDays) {
	}

	/**
	 * 지표 9. 데이터 충족도. 기록이 없는 날은 금연이 아니라 미확인으로 센다.
	 *
	 * @param targetDays         지난주 중 가입 뒤의 날 수(보통 7)
	 * @param observedDays       상태를 아는 날 수(smokedDays + smokeFreeDays)
	 * @param unknownDays        흡연 기록도 금연 확정도 없는 날 수
	 * @param coveragePercent    observedDays / targetDays × 100, 반올림
	 * @param sampleTargetDays   표본 기간 중 가입 뒤의 날 수
	 * @param sampleObservedDays 표본 기간 중 상태를 아는 날 수
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record DataSufficiency(DataSufficiencyLevel level, int targetDays, int observedDays, int smokedDays,
								  int smokeFreeDays, int unknownDays, int coveragePercent, int sampleTargetDays,
								  int sampleObservedDays) {
	}
}
