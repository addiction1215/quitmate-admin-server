package com.quitmate.firebase.enums;

import com.quitmate.alertHistory.entity.AlertDestinationType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PushMessage {

	DAILY_REPORT("일일 리포트"),
	FRIEND("친구 추가 알림"),
	FRIEND_CODE("친구 추가요청 수신알림"),
	NOTICE("공지사항 알림"),
	DEFAULT("Quitmate 알림");

	private final String text;

	public static PushMessage from(AlertDestinationType type) {
		return switch (type) {
			case DAILY_REPORT -> DAILY_REPORT;
			case FRIEND -> FRIEND;
			case FRIEND_CODE -> FRIEND_CODE;
			case NOTICE -> NOTICE;
		};
	}
}
