package com.quitmate.smokingPattern.enums;

/**
 * 지표 하나를 계산할 수 있었는지.
 */
public enum IndicatorStatus {
	/** 계산했다. */
	OK,
	/** 기록이 기준보다 적어 패턴이라고 말하기 어렵다. 숫자는 참고용으로만 남긴다. */
	INSUFFICIENT_DATA,
	/** 평소 하루 흡연량(기준값)을 알 수 없다. 지표 4에서만 쓴다. */
	NO_BASELINE,
	/** 계산에 필요한 기록(갈망 세션, 금연 확정)이 아예 없다. 지표 7·8에서만 쓴다. */
	NO_DATA
}
