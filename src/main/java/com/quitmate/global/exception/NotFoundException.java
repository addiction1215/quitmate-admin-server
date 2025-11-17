package com.quitmate.global.exception;

import lombok.Getter;

@Getter
public class NotFoundException extends RuntimeException {

	private final String message;

	public NotFoundException(String message, Exception e) {
		super(message, e);
		this.message = message;
	}

	public NotFoundException(String message) {
		this.message = message;
	}
}
