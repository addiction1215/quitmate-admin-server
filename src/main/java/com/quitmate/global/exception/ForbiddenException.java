package com.quitmate.global.exception;

import lombok.Getter;

/**
 * 로그인은 했지만 관리자 권한이 없는 요청. 403으로 돌려준다.
 */
@Getter
public class ForbiddenException extends RuntimeException {

	private final String message;

	public ForbiddenException(String message) {
		this.message = message;
	}
}
