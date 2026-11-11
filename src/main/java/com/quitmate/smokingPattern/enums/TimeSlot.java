package com.quitmate.smokingPattern.enums;

/**
 * 하루를 6시간씩 나눈 시간대. 지표 1(주요 흡연 시간대)에서 쓴다.
 */
public enum TimeSlot {
	/** 새벽 00~06시 */
	DAWN(0),
	/** 오전 06~12시 */
	MORNING(6),
	/** 오후 12~18시 */
	AFTERNOON(12),
	/** 저녁 18~24시 */
	EVENING(18);

	private final int startHour;

	TimeSlot(int startHour) {
		this.startHour = startHour;
	}

	public static TimeSlot of(int hour) {
		return values()[hour / 6];
	}

	public int startHour() {
		return startHour;
	}
}
