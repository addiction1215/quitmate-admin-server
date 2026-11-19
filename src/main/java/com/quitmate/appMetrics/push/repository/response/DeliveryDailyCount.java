package com.quitmate.appMetrics.push.repository.response;

import java.time.LocalDate;

/**
 * {@code push_delivery}(KAN-219, 기기 한 대에 보낸 푸시 한 건이 한 행)의 하루치 집계(created_date의 한국 날짜 기준).
 *
 * @param date           날짜
 * @param total          그날 생긴 행 전체
 * @param withoutOutbox  push_outbox를 거치지 않은 행(push_outbox_id 없음). 발송 요청 수에 더한다
 * @param ticketOk       ticket_status = OK
 * @param ticketError    ticket_status = ERROR
 * @param receiptOk      receipt_status = OK
 * @param receiptError   receipt_status = ERROR
 * @param receiptPending receipt_status = PENDING
 */
public record DeliveryDailyCount(LocalDate date, long total, long withoutOutbox, long ticketOk, long ticketError,
                                 long receiptOk, long receiptError, long receiptPending) {
}
