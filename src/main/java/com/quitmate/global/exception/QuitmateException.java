package com.quitmate.global.exception;

import lombok.Getter;

@Getter
public class QuitmateException extends RuntimeException {

	private final String message;

	public QuitmateException(String message, Exception e) {
		super(message, e);
		this.message = message;
	}

	public QuitmateException(String message) {
		this.message = message;
	}
}
