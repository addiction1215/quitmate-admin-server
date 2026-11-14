package com.quitmate.notice.service.impl;

import com.quitmate.expo.ExpoNotiService;
import com.quitmate.firebase.request.SendFirebaseServiceRequest;
import com.quitmate.notice.entity.Notice;
import com.quitmate.notice.enums.NoticePushStatus;
import com.quitmate.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 야간에 보류한 공지 푸시(DEFERRED)를 예정 시각 뒤에 보낸다(KAN-109). 스케줄러가 1분마다 부른다.
 *
 * <p>여러 대가 함께 돌아도(블루·그린 전환 중, dev·운영 서버가 같은 DB) 한 번만 보낸다.</p>
 * <ol>
 *   <li>보류 시간(20:00~08:00)에는 아무것도 보내지 않는다. 예정 시각이 지났어도 다음 08:00 까지 기다린다.</li>
 *   <li>보내기 직전에 조건부 UPDATE(DEFERRED → SENDING)로 집는다. 영향 행이 1인 서버만 보낸다.
 *       이 UPDATE 는 따로 커밋되어, 보내는 동안 다른 서버는 SENDING 을 본다.</li>
 *   <li>집은 뒤 공지를 다시 읽어 그때의 제목·내용·유형으로, 대상도 그때 다시 뽑아 보낸다. 그사이 지워졌으면 보내지 않는다(CANCELED).</li>
 *   <li>다 보내면 SENT. 보내다 오류가 나면 FAILED 로 두고 다시 보내지 않는다.</li>
 *   <li>서버가 보내다 멈춰 SENDING 으로 {@link #SENDING_TIMEOUT} 넘게 남은 행은 FAILED 로 바꾸고 다시 보내지 않는다.</li>
 * </ol>
 * 일부만 갔을 수 있는 경우에 다시 보내면 같은 사람이 두 번 받으므로, 중복 발송보다 누락을 택한다.
 *
 * <p>트랜잭션을 걸지 않는다. 상태 UPDATE 하나하나가 따로 커밋되어야 다른 서버가 바로 본다.
 * Expo 호출은 스케줄러 스레드에서 끝까지 기다린다(그래야 SENT 를 정확히 남긴다).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeferredNoticePushSender {

    /** SENDING 이 이보다 오래 남아 있으면 보내던 서버가 멈춘 것으로 본다. 정상 발송은 몇 분이면 끝난다 */
    static final Duration SENDING_TIMEOUT = Duration.ofMinutes(30);

    private final NoticeRepository noticeRepository;
    private final NoticePushRequestFactory noticePushRequestFactory;
    private final ExpoNotiService expoNotiService;
    private final AdPushQuietHours adPushQuietHours;

    /**
     * @return 이번에 보낸 공지 수
     */
    public int sendDuePushes() {
        failStalledPushes(adPushQuietHours.now());

        int sentCount = 0;
        LocalDateTime now = adPushQuietHours.now();
        if (adPushQuietHours.isQuiet(now)) {
            return sentCount;
        }
        for (Long noticeId : noticeRepository.findDeferredPushNoticeIdsDue(now)) {
            now = adPushQuietHours.now();
            if (adPushQuietHours.isQuiet(now)) {
                break; // 보내다 20:00 이 되면 남은 것은 다음 08:00 에 보낸다
            }
            try {
                if (sendOne(noticeId, now)) {
                    sentCount++;
                }
            } catch (RuntimeException e) {
                // SENT·FAILED 기록까지 실패한 경우다. SENDING 으로 남아 SENDING_TIMEOUT 뒤 FAILED 가 되고, 다시 보내지 않는다.
                log.error("보류한 공지 푸시 상태 기록 실패 - noticeId: {}, message: {}", noticeId, e.getMessage(), e);
            }
        }
        return sentCount;
    }

    private boolean sendOne(Long noticeId, LocalDateTime now) {
        if (noticeRepository.claimDeferredPush(noticeId, now) != 1) {
            return false; // 다른 서버가 먼저 집었거나, 그사이 지워졌다
        }

        int requestCount;
        try {
            Optional<Notice> notice = noticeRepository.findById(noticeId);
            if (notice.isEmpty()) {
                noticeRepository.changePushStatus(noticeId, NoticePushStatus.SENDING, NoticePushStatus.CANCELED);
                log.info("보류한 공지 푸시 취소(집은 뒤 공지 삭제) - noticeId: {}", noticeId);
                return false;
            }
            List<SendFirebaseServiceRequest> requests = noticePushRequestFactory.create(notice.get());
            requestCount = requests.size();
            expoNotiService.sendBatchPushNotification(requests);
        } catch (RuntimeException e) {
            log.error("보류한 공지 푸시 발송 실패 - 다시 보내지 않음, noticeId: {}, message: {}", noticeId, e.getMessage(), e);
            noticeRepository.changePushStatus(noticeId, NoticePushStatus.SENDING, NoticePushStatus.FAILED);
            return false;
        }

        if (noticeRepository.changePushStatus(noticeId, NoticePushStatus.SENDING, NoticePushStatus.SENT) != 1) {
            log.warn("보류한 공지 푸시는 보냈지만 SENT 로 바꾸지 못함(다른 서버가 FAILED 로 바꿨을 수 있음) - noticeId: {}", noticeId);
        }
        log.info("보류한 공지 푸시 발송 - noticeId: {}, 건수: {}", noticeId, requestCount);
        return true;
    }

    private void failStalledPushes(LocalDateTime now) {
        for (Long noticeId : noticeRepository.findSendingPushNoticeIdsStartedBefore(now.minus(SENDING_TIMEOUT))) {
            if (noticeRepository.changePushStatus(noticeId, NoticePushStatus.SENDING, NoticePushStatus.FAILED) == 1) {
                log.warn("공지 푸시가 {}분 넘게 발송 중으로 멈춰 있어 실패로 바꿈(일부만 갔을 수 있어 다시 보내지 않음) - noticeId: {}",
                        SENDING_TIMEOUT.toMinutes(), noticeId);
            }
        }
    }
}
