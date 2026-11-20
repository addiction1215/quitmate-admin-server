package com.quitmate.locationAccessLog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매일 새벽 04:40(한국 시각)에 보관 기간이 지난 위치정보 열람 이력을 지운다(KAN-208).
 * 예외는 여기서 잡는다. 스케줄러 스레드 밖으로 나가면 전역 예외 핸들러가 아니라 스케줄러 기본 로그로 샌다.
 * 관리자 서버에는 Slack 알림이 없어 ERROR 로그 한 줄(예외 클래스 이름만, 메시지·SQL 없이)만 남긴다.
 * 사용자 id·주소 등은 로그에 남기지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocationAccessLogRetentionScheduler {

    private final LocationAccessLogRetentionService retentionService;

    @Scheduled(cron = "0 40 4 * * *", zone = "Asia/Seoul")
    public void purgeExpiredLogs() {
        try {
            long deleted = retentionService.purgeExpired();
            log.info("위치정보 열람 이력 보관 기간 지난 행 삭제 - {}건", deleted);
        } catch (RuntimeException e) {
            log.error("위치정보 열람 이력 보관 기간 삭제 실패 - {}", e.getClass().getSimpleName());
        }
    }
}
