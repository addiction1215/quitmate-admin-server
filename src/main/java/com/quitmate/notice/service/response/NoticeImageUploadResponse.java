package com.quitmate.notice.service.response;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NoticeImageUploadResponse {

    /** S3 파일 키(notice/ 로 시작). 본문 img 의 data-key 에 넣는다. */
    private String key;
    /** 미리보기용 1시간 서명 URL. 본문 img 의 src 로 쓰되 저장은 data-key 만 남는다. */
    private String url;

    @Builder
    private NoticeImageUploadResponse(String key, String url) {
        this.key = key;
        this.url = url;
    }
}
