package com.quitmate.appMetrics.push.service.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 「앱 지표」 푸시 섹션 응답(KAN-221, KAN-217 계약 2번). 숫자를 셀 수 없으면 null(0과 다름).
 *
 * @param period           조회 기간(7·28·90)
 * @param startDate        시작 날짜(끝 날짜 포함 period일)
 * @param endDate          끝 날짜. 오늘(한국 날짜)
 * @param fetchedAt        DB를 실제로 읽은 시각. 캐시(1시간)에서 줄 때도 그 시각
 * @param deliveryTracking 기간 안에 push_delivery 행이 하나라도 있으면 true. false면 ticket·receipt 숫자는 null
 * @param openTracking     기간 안 하루 흡연 시간대 푸시 중 열린 것이 하나라도 있으면 true. false면 opened·openRate는 null
 * @param summary          기간 합계
 * @param daily            날짜별. 날짜 오름차순 period개, 빠진 날은 0(추적 전이면 null)
 * @param errorCodes       오류 코드별 건수(많은 순). 없으면 빈 배열
 */
public record PushMetricsResponse(
        int period,
        LocalDate startDate,
        LocalDate endDate,
        OffsetDateTime fetchedAt,
        boolean deliveryTracking,
        boolean openTracking,
        Summary summary,
        List<Daily> daily,
        List<ErrorCode> errorCodes
) {

    /**
     * @param requested      발송 요청(사람 단위): 기간에 생긴 push_outbox 행 수. push_delivery 는 더하지 않음
     * @param ticketOk       Expo 티켓 OK(push_delivery.ticket_status, 기기 단위)
     * @param ticketError    Expo 티켓 ERROR
     * @param receiptOk      Expo 영수증 OK(push_delivery.receipt_status, 기기 단위)
     * @param receiptError   Expo 영수증 ERROR
     * @param receiptPending 영수증 확인 전(PENDING)
     * @param opened         하루 흡연 시간대 푸시 중 열린 것(push_outbox.opened_at 있음)
     * @param openRate       opened / 보낸 하루 흡연 시간대 푸시(status = SENT 또는 열린 것). 0~1, 소수 넷째 자리
     * @param tokenUsers     푸시 토큰이 있는 회원 수(기간 무관, 지금 값)
     */
    public record Summary(
            long requested,
            Long ticketOk,
            Long ticketError,
            Long receiptOk,
            Long receiptError,
            Long receiptPending,
            Long opened,
            Double openRate,
            long tokenUsers
    ) {
    }

    public record Daily(LocalDate date, long requested, Long ticketError, Long receiptError, Long opened) {
    }

    public record ErrorCode(String code, long count) {
    }
}
