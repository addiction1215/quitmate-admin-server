package com.quitmate.notice.scheduler;

import com.quitmate.notice.service.impl.DeferredNoticePushSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 야간에 보류한 이벤트 공지 푸시를 보낸다(KAN-109).
 *
 * <p>앞 실행이 끝나고 1분 뒤마다 돈다. 08:00 이 지난 첫 실행에서 보내고, 08:00 에 서버가 꺼져 있었으면 켜진 뒤 첫 실행에서 보낸다.
 * 시각 판단은 {@code DeferredNoticePushSender} 가 한국 시각으로 한다. 켜는 설정은 {@code SchedulingConfig}(test 프로필 제외).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeferredNoticePushScheduler {

    private final DeferredNoticePushSender deferredNoticePushSender;

    @Scheduled(initialDelay = 1, fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    public void sendDeferredNoticePushes() {
        try {
            deferredNoticePushSender.sendDuePushes();
        } catch (RuntimeException e) {
            log.error("보류한 공지 푸시 스케줄 실행 실패 - message: {}", e.getMessage(), e);
        }
    }
}
