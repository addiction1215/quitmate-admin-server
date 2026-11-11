package com.quitmate.smokingPattern.enums;

/**
 * 지난주 7일 중 상태를 아는 날(흡연 기록 있음 또는 금연 확정)의 비율로 나눈 데이터 충족도.
 */
public enum DataSufficiencyLevel {
	/** 70% 이상 */
	SUFFICIENT,
	/** 40% 이상 70% 미만 */
	PARTIAL,
	/** 40% 미만 */
	INSUFFICIENT
}
