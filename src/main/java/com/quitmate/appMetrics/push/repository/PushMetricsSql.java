package com.quitmate.appMetrics.push.repository;

/**
 * 「앱 지표」 푸시 섹션(KAN-221) 숫자 정의와 SQL을 한 곳에 모은 것. <b>정의를 바꿀 때는 이 파일만 고친다.</b>
 * PR 본문의 검증 SELECT도 아래 정의와 같은 조건이다.
 *
 * <p>단위가 둘이다.
 * <ul>
 *   <li><b>발송 요청 = 사람 단위</b>: {@code push_outbox} 한 행이 한 사람에게 보낼 알림 하나다(기기가 여러 대여도 한 행).</li>
 *   <li><b>접수(ticket)·전달(receipt)·오류 코드 = 기기 단위</b>: {@code push_delivery}(KAN-219) 한 행이 토큰 하나에 보낸 한 건이다.
 *       outbox를 거치지 않는 친구 요청·수락 푸시도 들어 있다. 관리자 서버가 직접 보내는 공지·광고 푸시는 들어 있지 않다.</li>
 * </ul>
 * 두 표의 행 수는 단위가 달라 더하지 않는다.
 *
 * <p>날짜는 created_date(앱 서버가 한국 시각 LocalDateTime으로 넣음)의 날짜다. 기간은 {@code [:from, :to)}.
 * 운영(MySQL)과 테스트(H2, PostgreSQL 모드) 둘 다에서 도는 문법만 쓴다({@code day} 같은 예약어는 별칭으로 피함).
 */
final class PushMetricsSql {

    /** 오류 코드 목록 최대 길이 */
    static final int ERROR_CODE_LIMIT = 20;

    // ---------------------------------------------------------------------------------------------
    // 정의 (push_outbox, 사람 단위)
    // ---------------------------------------------------------------------------------------------

    /**
     * 하루 흡연 시간대 푸시. 앱 서버에서 schedule_id를 채우는 곳은 하루 흡연 배치(DailySmokingPushBatch,
     * destination DAILY_REPORT) 하나뿐이다. 주간 리포트·다음 고비 알림은 schedule_id 없이 dedupe_key로 만든다.
     */
    static final String DAILY_SMOKING = "schedule_id is not null";

    /** 발송 요청: 기간에 생긴 push_outbox 행 전부(종류·상태 무관). */
    static final String REQUESTED = "count(*)";

    /** 열림률 분모: 하루 흡연 시간대 푸시 중 보낸 것(SENT) 또는 이미 열린 것. 실패·취소·만료·대기 건은 사용자에게 안 갔으므로 뺀다. */
    static final String DAILY_SMOKING_SENT = countIf(DAILY_SMOKING + " and (status = 'SENT' or opened_at is not null)");

    /** 열림: 하루 흡연 시간대 푸시 중 opened_at이 있는 것. */
    static final String DAILY_SMOKING_OPENED = countIf(DAILY_SMOKING + " and opened_at is not null");

    // ---------------------------------------------------------------------------------------------
    // 정의 (push_delivery, 기기 단위)
    // ---------------------------------------------------------------------------------------------

    /** 전달 기록 전체. 기간에 하나라도 있으면 deliveryTracking = true. */
    static final String DELIVERY_ROWS = "count(*)";
    static final String TICKET_OK = countIf("ticket_status = 'OK'");
    static final String TICKET_ERROR = countIf("ticket_status = 'ERROR'");
    static final String RECEIPT_OK = countIf("receipt_status = 'OK'");
    static final String RECEIPT_ERROR = countIf("receipt_status = 'ERROR'");
    /** 영수증 확인 전. SKIPPED(티켓 오류이거나 24시간 지나도 영수증 없음)는 어느 숫자에도 넣지 않는다. */
    static final String RECEIPT_PENDING = countIf("receipt_status = 'PENDING'");

    /** 오류 코드 칸. Expo {@code details.error}(예: DeviceNotRegistered, 없으면 UNKNOWN). 빈 값은 뺀다. */
    static final String TICKET_ERROR_CODE = "ticket_error";
    static final String RECEIPT_ERROR_CODE = "receipt_error";

    // ---------------------------------------------------------------------------------------------
    // 정의 (push, 지금 값)
    // ---------------------------------------------------------------------------------------------

    /** 토큰 보유 회원: 토큰이 있는(빈 문자열 아님) 서로 다른 user_id. 탈퇴하면 앱 서버가 push 행을 지운다. */
    static final String TOKEN_USERS_WHERE = "user_id is not null and push_token is not null and push_token <> ''";

    // ---------------------------------------------------------------------------------------------
    // SQL
    // ---------------------------------------------------------------------------------------------

    static final String OUTBOX_DAILY = """
            select cast(created_date as date) as metric_date,
                   %s as requested,
                   %s as daily_smoking_sent,
                   %s as daily_smoking_opened
            from push_outbox
            where created_date >= :from and created_date < :to
            group by cast(created_date as date)
            """.formatted(REQUESTED, DAILY_SMOKING_SENT, DAILY_SMOKING_OPENED);

    static final String DELIVERY_DAILY = """
            select cast(created_date as date) as metric_date,
                   %s as total,
                   %s as ticket_ok,
                   %s as ticket_error,
                   %s as receipt_ok,
                   %s as receipt_error,
                   %s as receipt_pending
            from push_delivery
            where created_date >= :from and created_date < :to
            group by cast(created_date as date)
            """.formatted(DELIVERY_ROWS, TICKET_OK, TICKET_ERROR, RECEIPT_OK, RECEIPT_ERROR, RECEIPT_PENDING);

    static final String ERROR_CODES = """
            select error_code, count(*) as error_count
            from (
                select %1$s as error_code from push_delivery
                where created_date >= :from and created_date < :to
                  and %1$s is not null and %1$s <> ''
                union all
                select %2$s as error_code from push_delivery
                where created_date >= :from and created_date < :to
                  and %2$s is not null and %2$s <> ''
            ) errors_in_period
            group by error_code
            order by error_count desc, error_code asc
            limit %3$d
            """.formatted(TICKET_ERROR_CODE, RECEIPT_ERROR_CODE, ERROR_CODE_LIMIT);

    static final String TOKEN_USERS = """
            select count(distinct user_id)
            from push
            where %s
            """.formatted(TOKEN_USERS_WHERE);

    private PushMetricsSql() {
    }

    private static String countIf(String condition) {
        return "sum(case when " + condition + " then 1 else 0 end)";
    }
}
