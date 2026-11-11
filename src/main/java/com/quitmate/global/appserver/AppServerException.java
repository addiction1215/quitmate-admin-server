package com.quitmate.global.appserver;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 앱 서버 호출이 실패했을 때. 관리자 화면에 돌려줄 상태 코드와 사유를 담는다.
 */
@Getter
public class AppServerException extends RuntimeException {

    private final HttpStatus status;
    private final String message;

    public AppServerException(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public AppServerException(HttpStatus status, String message, Exception cause) {
        super(message, cause);
        this.status = status;
        this.message = message;
    }
}
