package com.quitmate.notice.service;

import com.quitmate.notice.service.request.NoticeCreateServiceRequest;
import com.quitmate.notice.service.request.NoticeUpdateServiceRequest;
import com.quitmate.notice.service.response.NoticeCreateResponse;
import com.quitmate.notice.service.response.NoticeImageUploadResponse;
import com.quitmate.notice.service.response.NoticeUpdateResponse;
import org.springframework.web.multipart.MultipartFile;

public interface NoticeService {

    NoticeCreateResponse createNotice(NoticeCreateServiceRequest request);

    NoticeUpdateResponse updateNotice(NoticeUpdateServiceRequest request);

    void deleteNotice(Long id);

    NoticeImageUploadResponse uploadNoticeImage(MultipartFile file);
}
