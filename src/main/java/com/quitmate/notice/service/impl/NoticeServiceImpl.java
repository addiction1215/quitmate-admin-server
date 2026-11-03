package com.quitmate.notice.service.impl;

import com.quitmate.alertHistory.entity.AlertDestinationType;
import com.quitmate.alertSetting.entity.AlertSetting;
import com.quitmate.alertSetting.entity.enums.AlertType;
import com.quitmate.alertSetting.service.AlertSettingReadService;
import com.quitmate.expo.event.PushNotificationEvent;
import com.quitmate.firebase.request.SendFirebaseDataDto;
import com.quitmate.firebase.request.SendFirebaseServiceRequest;
import com.quitmate.global.exception.QuitmateException;
import com.quitmate.notice.entity.Notice;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    public NoticeCreateResponse createNotice(NoticeCreateServiceRequest request) {
        NoticeCreateResponse noticeCreateResponse = NoticeCreateResponse.createResponse(noticeRepository.save(request.toEntity()));

        List<User> users = userReadService.findAllWithPushes();

        SendFirebaseDataDto dataDto = SendFirebaseDataDto.builder()
                .alert_destination_type(AlertDestinationType.NOTICE)
                .alert_destination_info("공지사항")
                .build();

        String pushBody = toPushBody(noticeCreateResponse.getContent());
        List<SendFirebaseServiceRequest> pushRequests = new ArrayList<>();
        for (User user : users) {
            if (!shouldSendPush(user)) continue;
            List<Push> pushes = getPushToken(user);
            if (pushes.isEmpty()) continue;

            pushes.forEach(push -> pushRequests.add(
                    SendFirebaseServiceRequest.builder()
                            .push(push)
                            .title(noticeCreateResponse.getTitle())
                            .body(pushBody)
                            .sound("default")
                            .sendFirebaseDataDto(dataDto)
                            .build()
            ));
        }

        if (!pushRequests.isEmpty()) {
            eventPublisher.publishEvent(new PushNotificationEvent(pushRequests));
        }
        return noticeCreateResponse;
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
     * 사용자에게 Push 알림을 전송해야 하는지 확인
     * - 전체 알림 설정 확인
     * - 리포트 알림 설정 확인
     */
    private boolean shouldSendPush(User user) {
        AlertSetting alertSetting = alertSettingReadService.findByUserOrCreateDefault(user);

        if (alertSetting == null) {
            return false;
        }

        // 전체 알림이 OFF면 전송하지 않음
        if (alertSetting.getAll() == AlertType.OFF) {
            return false;
        }

        // 리포트 알림이 OFF면 전송하지 않음
        return alertSetting.getReport() != AlertType.OFF;
    }


    private String toPushBody(String content) {
        if (content.codePointCount(0, content.length()) <= PUSH_BODY_MAX_LENGTH) {
            return content;
        }
        return content.substring(0, content.offsetByCodePoints(0, PUSH_BODY_MAX_LENGTH - 1)) + "…";
    }

    /**
     * 사용자의 Push 토큰 조회
     */
    private List<Push> getPushToken(User user) {
        return user.getPushes() != null ? user.getPushes() : List.of();
    }

}
