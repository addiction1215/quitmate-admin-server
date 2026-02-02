package com.quitmate.alertHistory.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AlertDestinationType {

    DAILY_REPORT("일일 리포트"),
	FRIEND("친구 추가 알림"),
	FRIEND_CODE("친구 추가요청 수신알림"),
	NOTICE("공지사항");

	private final String text;
}
