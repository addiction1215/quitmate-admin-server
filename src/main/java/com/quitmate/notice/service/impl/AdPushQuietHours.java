package com.quitmate.notice.service.impl;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * 이벤트 공지(광고성 앱 푸시)를 바로 보내지 않고 보류하는 시간: 한국 시각 {@link #START} 이상, {@link #END} 미만.
 *
 * <p>정보통신망법 제50조 제3항은 21:00~08:00 에 광고성 정보를 보내려면 별도 사전 동의를 요구하고, QuitMate 는 받지 않는다.
 * 여유를 두어 20:00 부터 보류한다(KAN-109, 정현님 11/14 "8시(20시)로 부탁합니다").
 * 이 시간에 등록한 이벤트 공지의 푸시는 다음 {@link #END} 이후 스케줄러가 보낸다({@link DeferredNoticePushSender}).</p>
 *
 * <p>서버 시간대와 상관없이 {@link Clock#instant()} 를 한국 시각으로 바꿔 판단한다. 시계는 테스트에서 바꿔 끼운다.</p>
 */
@Component
public class AdPushQuietHours {

    public static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    /** 보류를 시작하는 시각. 이 값 하나로 판단·등록 응답 안내 문구가 함께 바뀐다 */
    public static final LocalTime START = LocalTime.of(20, 0);
    /** 보류가 끝나 보류분을 보내기 시작하는 시각. START 보다 이른 시각이어야 한다(보류 시간이 자정을 넘는다) */
    public static final LocalTime END = LocalTime.of(8, 0);

    private final Clock clock;

    public AdPushQuietHours(Clock clock) {
        this.clock = clock;
    }

    /** 지금 한국 시각 */
    public LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), KOREA);
    }

    /** 한국 시각 {@code koreaTime} 이 보류 시간인지 */
    public boolean isQuiet(LocalDateTime koreaTime) {
        LocalTime time = koreaTime.toLocalTime();
        return !time.isBefore(START) || time.isBefore(END);
    }

    /** 한국 시각 {@code koreaTime} 에 등록한 광고 푸시를 보낼 수 있는 가장 이른 시각. 보류 시간이 아니면 그 시각 그대로 */
    public LocalDateTime nextSendableAt(LocalDateTime koreaTime) {
        if (!isQuiet(koreaTime)) {
            return koreaTime;
        }
        LocalDate date = koreaTime.toLocalTime().isBefore(END)
                ? koreaTime.toLocalDate()
                : koreaTime.toLocalDate().plusDays(1);
        return date.atTime(END);
    }

    /** 안내 문구용 "20:00~08:00" */
    public static String label() {
        return START + "~" + END;
    }
}
