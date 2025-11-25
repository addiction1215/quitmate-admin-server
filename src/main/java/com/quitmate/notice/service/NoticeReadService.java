package com.quitmate.notice.service;

import com.quitmate.global.page.response.PageCustom;
import com.quitmate.notice.service.request.NoticeListServiceRequest;
import com.quitmate.notice.service.response.NoticeListResponse;

public interface NoticeReadService {

    PageCustom<NoticeListResponse> getNoticeList(NoticeListServiceRequest request);
}
