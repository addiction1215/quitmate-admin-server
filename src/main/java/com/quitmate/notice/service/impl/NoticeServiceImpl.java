package com.quitmate.notice.service.impl;

import com.quitmate.expo.event.PushNotificationEvent;
import com.quitmate.firebase.request.SendFirebaseServiceRequest;
import com.quitmate.global.exception.QuitmateException;
import com.quitmate.notice.entity.Notice;
import com.quitmate.notice.enums.NoticePushStatus;
import com.quitmate.notice.repository.NoticeRepository;
import com.quitmate.notice.service.NoticeService;
import com.quitmate.notice.service.request.NoticeCreateServiceRequest;
import com.quitmate.notice.service.request.NoticeUpdateServiceRequest;
import com.quitmate.notice.service.response.NoticeCreateResponse;
import com.quitmate.notice.service.response.NoticeUpdateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NoticeServiceImpl implements NoticeService {

    private static final String UNKNOWN_NOTICE = "해당 공지사항은 존재하지 않습니다.";

    private final ApplicationEventPublisher eventPublisher;

    private final NoticeRepository noticeRepository;
    private final NoticePushRequestFactory noticePushRequestFactory;
    private final AdPushQuietHours adPushQuietHours;

    /**
     * 공지를 등록하고 푸시를 보낸다. 야간(20:00~08:00)에 등록한 이벤트 공지는 푸시를 보류하고(DEFERRED),
     * 다음 08:00 이후 {@link DeferredNoticePushSender} 가 그때의 최신 제목·내용·대상으로 보낸다(KAN-109).
     */
    @Override
    public NoticeCreateResponse createNotice(NoticeCreateServiceRequest request) {
        LocalDateTime now = adPushQuietHours.now();
        Notice notice = request.toEntity();

        if (notice.getType().isAdvertising() && adPushQuietHours.isQuiet(now)) {
            LocalDateTime scheduledAt = adPushQuietHours.nextSendableAt(now);
            notice.deferPush(scheduledAt);
            noticeRepository.save(notice);
            log.info("야간 이벤트 공지 - 광고 푸시 보류, noticeId: {}, 보낼 시각: {}", notice.getId(), scheduledAt);
            return NoticeCreateResponse.createResponse(notice, AdPushCopy.deferredPushReason(scheduledAt));
        }

        notice.markPushSent(now);
        noticeRepository.save(notice);
        List<SendFirebaseServiceRequest> pushRequests = noticePushRequestFactory.create(notice);
        if (!pushRequests.isEmpty()) {
            eventPublisher.publishEvent(new PushNotificationEvent(pushRequests));
        }
        return NoticeCreateResponse.createResponse(notice);
    }

    /**
     * 공지를 고친다. 푸시를 보류한 공지는 보낼 때 고친 제목·내용·유형으로 보낸다. 이미 보낸 푸시는 다시 보내지 않는다.
     */
    @Override
    public NoticeUpdateResponse updateNotice(NoticeUpdateServiceRequest request) {
        Notice notice = noticeRepository.findById(request.getId())
                .orElseThrow(() -> new QuitmateException(UNKNOWN_NOTICE));

        notice.update(request.getType(), request.getTitle(), request.getContent());
        return NoticeUpdateResponse.createResponse(notice);
    }

    /**
     * 공지를 지운다. 아직 보내지 않은 보류 푸시는 취소한다(CANCELED).
     */
    @Override
    public void deleteNotice(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new QuitmateException(UNKNOWN_NOTICE));

        notice.delete();
        if (noticeRepository.changePushStatus(id, NoticePushStatus.DEFERRED, NoticePushStatus.CANCELED) == 1) {
            log.info("보류한 공지 푸시 취소(공지 삭제) - noticeId: {}", id);
        }
    }
}
