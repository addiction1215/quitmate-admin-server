package com.quitmate.appMetrics.push.repository.response;

/**
 * ticket_error·receipt_error를 합친 오류 코드별 건수.
 */
public record ErrorCodeCount(String code, long count) {
}
