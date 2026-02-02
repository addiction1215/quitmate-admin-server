package com.quitmate.notice.service.impl;

import com.quitmate.alertHistory.entity.AlertDestinationType;
import com.quitmate.alertSetting.entity.AlertSetting;
import com.quitmate.alertSetting.entity.enums.AlertType;
import com.quitmate.alertSetting.service.AlertSettingReadService;
import com.quitmate.firebase.FirebaseService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NoticeServiceImpl implements NoticeService {

    private static final String UNKNOWN_NOTICE = "해당 공지사항은 존재하지 않습니다.";

    private final FirebaseService firebaseService;
    private final UserReadService userReadService;

    private final NoticeRepository noticeRepository;
    private final AlertSettingReadService alertSettingReadService;

    @Override
    public NoticeCreateResponse createNotice(NoticeCreateServiceRequest request) {
        NoticeCreateResponse noticeCreateResponse = NoticeCreateResponse.createResponse(noticeRepository.save(request.toEntity()));

        List<User> users = userReadService.findAllWithPushes();

        for (User user : users) {
            // 알림 설정 확인
            if (!shouldSendPush(user)) {
                continue;
            }
            List<Push> pushes = getPushToken(user);
            if (pushes.isEmpty()) {
                continue;
            }
            for (Push push : pushes) {
                if (push == null) {
                    continue;
                }
                // Push 알림 전송
                sendPushNotification(noticeCreateResponse.getContent(), push);
            }
        }
        return noticeCreateResponse;
    }

    @Override
    public NoticeUpdateResponse updateNotice(NoticeUpdateServiceRequest request) {
        Notice notice = noticeRepository.findById(request.getId())
                .orElseThrow(() -> new QuitmateException(UNKNOWN_NOTICE));

        notice.update(request.getType(), request.getContent());
        return NoticeUpdateResponse.createResponse(notice);
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


    /**
     * 사용자의 Push 토큰 조회
     */
    private List<Push> getPushToken(User user) {
        return user.getPushes() != null ? user.getPushes() : List.of();
    }

    private void sendPushNotification(String message, Push push) {
        SendFirebaseDataDto dataDto = SendFirebaseDataDto.builder()
                .alert_destination_type(AlertDestinationType.NOTICE)
                .alert_destination_info("데일리 리포트")
                .build();

        SendFirebaseServiceRequest serviceRequest = SendFirebaseServiceRequest.builder()
                .push(push)
                .body(message)
                .sound("default")
                .sendFirebaseDataDto(dataDto)
                .build();

        firebaseService.sendPushNotification(serviceRequest);
    }
}
