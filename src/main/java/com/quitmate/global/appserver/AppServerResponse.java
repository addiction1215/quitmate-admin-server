package com.quitmate.global.appserver;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 앱 서버 ApiResponse 모양. 성공이면 data, 실패면 message에 사유가 온다.
 */
@Getter
@NoArgsConstructor
public class AppServerResponse<T> {

    private int statusCode;
    private String message;
    private T data;
}
