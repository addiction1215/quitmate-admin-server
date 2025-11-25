package com.quitmate.notice.service;

import com.quitmate.notice.service.request.NoticeCreateServiceRequest;
import com.quitmate.notice.service.request.NoticeUpdateServiceRequest;
import com.quitmate.notice.service.response.NoticeCreateResponse;
import com.quitmate.notice.service.response.NoticeUpdateResponse;

public interface NoticeService {

    NoticeCreateResponse createNotice(NoticeCreateServiceRequest request);

    NoticeUpdateResponse updateNotice(NoticeUpdateServiceRequest request);
}
