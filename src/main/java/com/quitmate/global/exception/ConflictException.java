package com.quitmate.global.exception;

import lombok.Getter;

/**
 * 지금 상태에서는 할 수 없는 요청(이미 처리된 건, 겹치는 기간 등). 409로 돌려준다.
 */
@Getter
public class ConflictException extends RuntimeException {

	private final String message;

	public ConflictException(String message) {
		this.message = message;
	}
}
