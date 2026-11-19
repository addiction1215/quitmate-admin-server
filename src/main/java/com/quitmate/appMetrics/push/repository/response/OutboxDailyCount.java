package com.quitmate.appMetrics.push.repository.response;

import java.time.LocalDate;

/**
 * {@code push_outbox}의 하루치 집계(생성 시각 created_date의 한국 날짜 기준).
 *
 * @param date              날짜
 * @param requested         그날 생긴 대기 건 전체(하루 흡연 시간대·주간 리포트·다음 고비 알림 등 상태 무관)
 * @param dailySmokingSent  하루 흡연 시간대 푸시(schedule_id 있음) 중 보낸 것(status = SENT) 또는 열린 것. 열림률 분모
 * @param dailySmokingOpened 하루 흡연 시간대 푸시 중 열린 것(opened_at 있음)
 */
public record OutboxDailyCount(LocalDate date, long requested, long dailySmokingSent, long dailySmokingOpened) {
}
