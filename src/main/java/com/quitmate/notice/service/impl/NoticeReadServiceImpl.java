package com.quitmate.notice.service.impl;

import com.quitmate.global.page.response.PageCustom;
import com.quitmate.global.exception.QuitmateException;
import com.quitmate.notice.entity.Notice;
import com.quitmate.notice.html.NoticeHtmlSanitizer;
import com.quitmate.notice.repository.NoticeRepository;
import com.quitmate.notice.service.NoticeReadService;
import com.quitmate.notice.service.request.NoticeListServiceRequest;
import com.quitmate.notice.service.response.NoticeDetailResponse;
import com.quitmate.notice.service.response.NoticeListResponse;
import com.quitmate.storage.enums.BucketKind;
import com.quitmate.storage.service.S3StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeReadServiceImpl implements NoticeReadService {

    private final NoticeRepository noticeRepository;
    private final S3StorageService s3StorageService;

    @Override
    public PageCustom<NoticeListResponse> getNoticeList(NoticeListServiceRequest request) {
        return PageCustom.of(noticeRepository.findNoticeList(request, request.toPageable()).map(NoticeListResponse::createResponse));
    }

    @Override
    public NoticeDetailResponse getNotice(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new QuitmateException("해당 공지사항은 존재하지 않습니다."));
        String html = NoticeHtmlSanitizer.fillImageSrc(notice.getContentHtml(),
                key -> s3StorageService.createPresignedUrl(key, BucketKind.NOTICE));
        return NoticeDetailResponse.createResponse(notice, html);
    }
}
