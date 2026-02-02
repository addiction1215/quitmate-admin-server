package com.quitmate.firebase.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PushMessage {

	TITLE("Addiction 알림"),
	FRIEND("친구 추가 알림");

	private final String text;

}
