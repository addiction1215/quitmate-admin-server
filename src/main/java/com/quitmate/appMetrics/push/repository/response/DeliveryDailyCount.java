package com.quitmate.appMetrics.push.repository.response;

import java.time.LocalDate;

/**
 * {@code push_delivery}(KAN-219, 기기(토큰) 하나에 보낸 한 건이 한 행)의 하루치 집계(created_date의 한국 날짜 기준).
 * 기기 단위라 발송 요청(사람 단위, push_outbox)에는 더하지 않는다.
 *
 * @param date           날짜
 * @param total          그날 생긴 행 전체(친구 푸시처럼 outbox를 거치지 않은 것 포함)
 * @param ticketOk       ticket_status = OK
 * @param ticketError    ticket_status = ERROR
 * @param receiptOk      receipt_status = OK
 * @param receiptError   receipt_status = ERROR
 * @param receiptPending receipt_status = PENDING
 */
public record DeliveryDailyCount(LocalDate date, long total, long ticketOk, long ticketError,
                                 long receiptOk, long receiptError, long receiptPending) {
}
