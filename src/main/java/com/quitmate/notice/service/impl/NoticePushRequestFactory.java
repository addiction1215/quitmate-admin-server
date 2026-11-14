package com.quitmate.notice.service.impl;

import com.quitmate.alertHistory.entity.AlertDestinationType;
import com.quitmate.alertSetting.entity.AlertSetting;
import com.quitmate.alertSetting.service.AlertSettingReadService;
import com.quitmate.firebase.request.SendFirebaseDataDto;
import com.quitmate.firebase.request.SendFirebaseServiceRequest;
import com.quitmate.notice.entity.Notice;
import com.quitmate.notice.enums.NoticeType;
import com.quitmate.user.push.entity.Push;
import com.quitmate.user.users.entity.User;
import com.quitmate.user.users.service.UserReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 공지 푸시의 대상·문구를 만든다(KAN-109). 등록하며 바로 보낼 때와, 보류했다가 스케줄러가 보낼 때 같은 규칙을 쓴다.
 * 대상은 부를 때마다 새로 뽑는다. 보류한 사이 광고 푸시 동의를 철회한 사람은 빠지고, 새로 동의한 사람은 들어간다.
 */
@Component
@RequiredArgsConstructor
public class NoticePushRequestFactory {

    // 푸시 본문이 알림 기록(alert_history.alert_description, varchar(255))에 그대로 들어가 넘으면 기록 저장이 실패한다.
    private static final int PUSH_BODY_MAX_LENGTH = 255;

    private final UserReadService userReadService;
    private final AlertSettingReadService alertSettingReadService;

    @Transactional(readOnly = true)
    public List<SendFirebaseServiceRequest> create(Notice notice) {
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

    /**
     * 공지 유형별로 푸시를 받을 사람인지 확인한다(KAN-109).
     * - 안내·업데이트·점검: 서비스 소식 ON
     * - 이벤트: 광고성 앱 푸시 수신 동의(AGREED). 미동의·철회는 받지 않는다.
     * 알림 설정 행이 없으면 앱 서버 기본값(서비스 소식 ON, 광고 미동의)으로 보고, 행을 만들지 않는다.
     * 옛 all·report 칸은 보지 않는다. 예전 거부는 KAN-107 DDL의 일회성 UPDATE로 새 칸에 옮겨 둔다.
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
