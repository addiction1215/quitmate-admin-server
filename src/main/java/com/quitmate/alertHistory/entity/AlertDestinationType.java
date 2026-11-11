package com.quitmate.alertHistory.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AlertDestinationType {

    DAILY_REPORT("일일 리포트"),
	FRIEND("친구 추가 알림"),
	FRIEND_CODE("친구 추가요청 수신알림"),
	NOTICE("공지사항"),
	INQUIRY("1:1 문의"),
	// 앱 서버 주간 흡연 패턴 리포트 알림(KAN-146). 관리자 서버는 보내지 않지만 alert_history에 이 값이 들어오므로 읽을 수 있어야 한다.
	WEEKLY_REPORT("주간 흡연 패턴 리포트");

	private final String text;
}
