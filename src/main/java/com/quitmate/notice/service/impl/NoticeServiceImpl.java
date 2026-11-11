package com.quitmate.notice.service.impl;

import com.quitmate.alertHistory.entity.AlertDestinationType;
import com.quitmate.alertSetting.entity.AlertSetting;
import com.quitmate.alertSetting.service.AlertSettingReadService;
import com.quitmate.expo.event.PushNotificationEvent;
import com.quitmate.firebase.request.SendFirebaseDataDto;
import com.quitmate.firebase.request.SendFirebaseServiceRequest;
import com.quitmate.global.exception.QuitmateException;
import com.quitmate.notice.entity.Notice;
import com.quitmate.notice.enums.NoticeType;
import com.quitmate.notice.repository.NoticeRepository;
import com.quitmate.notice.service.NoticeService;
import com.quitmate.notice.service.request.NoticeCreateServiceRequest;
import com.quitmate.notice.service.request.NoticeUpdateServiceRequest;
import com.quitmate.notice.service.response.NoticeCreateResponse;
import com.quitmate.notice.service.response.NoticeUpdateResponse;
import com.quitmate.user.push.entity.Push;
import com.quitmate.user.users.entity.User;
import com.quitmate.user.users.service.UserReadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NoticeServiceImpl implements NoticeService {

    private static final String UNKNOWN_NOTICE = "해당 공지사항은 존재하지 않습니다.";
    // 푸시 본문이 알림 기록(alert_history.alert_description, varchar(255))에 그대로 들어가 넘으면 기록 저장이 실패한다.
    private static final int PUSH_BODY_MAX_LENGTH = 255;

    private final ApplicationEventPublisher eventPublisher;
    private final UserReadService userReadService;

    private final NoticeRepository noticeRepository;
    private final AlertSettingReadService alertSettingReadService;
    private final AdPushQuietHours adPushQuietHours;

    @Override
    public NoticeCreateResponse createNotice(NoticeCreateServiceRequest request) {
        Notice notice = noticeRepository.save(request.toEntity());

        if (notice.getType().isAdvertising() && adPushQuietHours.isQuietNow()) {
            // 지연 발송은 아직 없다(정현님과 방식 결정 전). 공지만 등록하고 푸시는 보내지 않는다.
            log.info("야간 이벤트 공지 - 광고 푸시 미발송, noticeId: {}", notice.getId());
            return NoticeCreateResponse.createResponse(notice, AdPushCopy.QUIET_HOURS_SKIP_REASON);
        }

        List<SendFirebaseServiceRequest> pushRequests = buildPushRequests(notice);
        if (!pushRequests.isEmpty()) {
            eventPublisher.publishEvent(new PushNotificationEvent(pushRequests));
        }
        return NoticeCreateResponse.createResponse(notice);
    }

    private List<SendFirebaseServiceRequest> buildPushRequests(Notice notice) {
        List<User> users = userReadService.findAllWithPushes();
        Map<Long, AlertSetting> settingsByUserId = alertSettingReadService.findAllByUsers(users);

        SendFirebaseDataDto dataDto = SendFirebaseDataDto.builder()
                .alert_destination_type(AlertDestinationType.NOTICE)
                .alert_destination_info("공지사항")
                .notice_id(notice.getId())
                .build();
        String pushTitle = toPushTitle(notice);
        String pushBody = toPushBody(notice);

        List<SendFirebaseServiceRequest> pushRequests = new ArrayList<>();
        for (User user : users) {
            if (!shouldSendPush(settingsByUserId.get(user.getId()), notice.getType())) continue;
            List<Push> pushes = getPushToken(user);
            if (pushes.isEmpty()) continue;

            pushes.forEach(push -> pushRequests.add(
                    SendFirebaseServiceRequest.builder()
                            .push(push)
                            .title(pushTitle)
                            .body(pushBody)
                            .sound("default")
                            .sendFirebaseDataDto(dataDto)
                            .build()
            ));
        }
        return pushRequests;
    }

    @Override
    public NoticeUpdateResponse updateNotice(NoticeUpdateServiceRequest request) {
        Notice notice = noticeRepository.findById(request.getId())
                .orElseThrow(() -> new QuitmateException(UNKNOWN_NOTICE));

        notice.update(request.getType(), request.getTitle(), request.getContent());
        return NoticeUpdateResponse.createResponse(notice);
    }

    @Override
    public void deleteNotice(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new QuitmateException(UNKNOWN_NOTICE));

        notice.delete();
    }

    /**
     * 공지 유형별로 푸시를 받을 사람인지 확인한다(KAN-109).
     * - 안내·업데이트·점검: 서비스 소식 ON
     * - 이벤트: 광고성 앱 푸시 수신 동의(AGREED). 미동의·철회는 받지 않는다.
     * 알림 설정 행이 없으면 앱 서버 기본값(서비스 소식 ON, 광고 미동의)으로 보고, 행을 만들지 않는다.
     */
    private boolean shouldSendPush(AlertSetting alertSetting, NoticeType type) {
        if (alertSetting == null) {
            return !type.isAdvertising();
        }
        return type.isAdvertising() ? alertSetting.agreedToAdPush() : alertSetting.receivesServiceNews();
    }

    private String toPushTitle(Notice notice) {
        return notice.getType().isAdvertising() ? AdPushCopy.title(notice.getTitle()) : notice.getTitle();
    }

    /**
     * 푸시 본문은 알림 기록 칸(255자)에 맞춘다. 이벤트 공지는 끝에 전송자·수신 거부 방법을 붙이고, 그만큼 내용을 줄인다.
     */
    private String toPushBody(Notice notice) {
        if (!notice.getType().isAdvertising()) {
            return truncate(notice.getContent(), PUSH_BODY_MAX_LENGTH);
        }
        String footer = AdPushCopy.BODY_FOOTER;
        int contentMaxLength = PUSH_BODY_MAX_LENGTH - footer.codePointCount(0, footer.length());
        return truncate(notice.getContent(), contentMaxLength) + footer;
    }

    private String truncate(String content, int maxLength) {
        if (content.codePointCount(0, content.length()) <= maxLength) {
            return content;
        }
        return content.substring(0, content.offsetByCodePoints(0, maxLength - 1)) + "…";
    }

    /**
     * 사용자의 Push 토큰 조회
     */
    private List<Push> getPushToken(User user) {
        return user.getPushes() != null ? user.getPushes() : List.of();
    }

}
