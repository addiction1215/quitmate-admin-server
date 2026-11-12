package com.quitmate.firebase.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.quitmate.alertHistory.entity.AlertDestinationType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class SendFirebaseDataDto {

    private AlertDestinationType alert_destination_type;
    private String alert_destination_info;
    /**
     * 공지 푸시일 때 공지 ID. 앱이 푸시를 눌렀을 때 공지 상세로 보내는 데 쓴다.
     * alert_destination_info 는 알림 기록에 그대로 남아 앱 알림 상세 제목("공지사항")으로 쓰이므로 따로 둔다.
     * 공지가 아닌 푸시에는 키를 넣지 않는다.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long notice_id;

    @Builder
    private SendFirebaseDataDto(AlertDestinationType alert_destination_type, String alert_destination_info, Long notice_id) {
        this.alert_destination_type = alert_destination_type;
        this.alert_destination_info = alert_destination_info;
        this.notice_id = notice_id;
    }
}
