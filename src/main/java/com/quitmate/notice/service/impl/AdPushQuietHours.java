package com.quitmate.notice.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * 광고성 앱 푸시를 보내지 않는 야간(한국 시각 21:00 이상, 08:00 미만).
 * 정보통신망법 제50조 제3항: 이 시간에 광고성 정보를 보내려면 별도 사전 동의가 필요하고, QuitMate 는 받지 않는다.
 */
@Component
public class AdPushQuietHours {

    static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    static final LocalTime START = LocalTime.of(21, 0);
    static final LocalTime END = LocalTime.of(8, 0);

    private final Clock clock;

    @Autowired
    public AdPushQuietHours() {
        this(Clock.system(KOREA));
    }

    AdPushQuietHours(Clock clock) {
        this.clock = clock;
    }

    public boolean isQuietNow() {
        LocalTime now = LocalTime.now(clock.withZone(KOREA));
        return !now.isBefore(START) || now.isBefore(END);
    }
}
