package com.quitmate.global.jwt.exception;

import lombok.Getter;

/*
 * JwtTokenException은 HttpStatus가 500이 아니라 401로 내려줘야하기에 따로 생성
 * */
@Getter
public class JwtTokenException extends RuntimeException {
	private final String message;

	public JwtTokenException(String message, Exception e) {
		super(message, e);
		this.message = message;
	}

	public JwtTokenException(String message) {
		this.message = message;
	}
}
