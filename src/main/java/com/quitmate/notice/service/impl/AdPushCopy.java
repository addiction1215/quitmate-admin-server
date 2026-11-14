package com.quitmate.notice.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 광고성 앱 푸시(이벤트 공지)에 붙이는 문구. 정보통신망법 제50조의 (광고) 표시·전송자·수신 거부 방법.
 *
 * <p>문구 확정 전: KAN-82 법무 검토 결과에 따라 바꾼다. 문구는 이 파일에만 둔다.</p>
 */
public final class AdPushCopy {

    /** 제목 맨 앞에 붙인다 */
    public static final String TITLE_PREFIX = "(광고) ";

    public static final String SENDER = "QuitMate";

    /**
     * 전송자 연락처. 광고성 앱 푸시도 전화번호나 주소를 함께 적어야 한다(KISA 정보통신망법 안내서 7차 개정본).
     * KAN-109에서 운영팀이 정한 번호다.
     */
    public static final String SENDER_CONTACT = "010-5093-9902";

    public static final String OPT_OUT_GUIDE = "설정 > 알림 설정에서 끌 수 있어요";

    /** 본문 맨 끝에 붙인다. 본문이 길면 공지 내용 쪽을 줄여 이 문구는 늘 남긴다 */
    public static final String BODY_FOOTER =
            "\n\n전송자: " + SENDER + " (" + SENDER_CONTACT + ")\n수신 거부: " + OPT_OUT_GUIDE;

    private static final DateTimeFormatter SCHEDULED_AT_FORMAT = DateTimeFormatter.ofPattern("M월 d일 HH:mm");

    private AdPushCopy() {
    }

    public static String title(String title) {
        return TITLE_PREFIX + title;
    }

    /** 야간에 이벤트 공지를 등록해 푸시를 보류했을 때 관리자에게 돌려주는 안내(등록 응답 pushSkipReason) */
    public static String deferredPushReason(LocalDateTime scheduledAt) {
        return "야간(" + AdPushQuietHours.label() + ")에는 광고 푸시를 바로 보내지 않아요. "
                + "공지는 등록했고, 푸시는 " + scheduledAt.format(SCHEDULED_AT_FORMAT) + "에 보내요.";
    }
}
