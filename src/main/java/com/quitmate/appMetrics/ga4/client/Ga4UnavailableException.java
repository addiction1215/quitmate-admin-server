package com.quitmate.appMetrics.ga4.client;

/**
 * GA4에서 지표를 받지 못했을 때(권한·쿼터·네트워크·응답 이상, 키 파일 문제). 503 + message {@value #RESPONSE_MESSAGE}로 돌려준다.
 *
 * <p>예외 메시지(사유)는 서버 로그에만 남고 응답에는 나가지 않는다. 사유에 키·토큰·속성 ID를 넣지 않는다.
 */
public class Ga4UnavailableException extends RuntimeException {

    /** 응답 message. 관리자 화면은 이 값으로 'GA4 응답 실패'를 띄운다(KAN-217 계약). */
    public static final String RESPONSE_MESSAGE = "GA4_UNAVAILABLE";

    public Ga4UnavailableException(String reason) {
        super(reason);
    }

    public Ga4UnavailableException(String reason, Throwable cause) {
        super(reason, cause);
    }
}
