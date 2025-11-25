package com.quitmate.notice.service.impl;

import com.quitmate.global.page.response.PageCustom;
import com.quitmate.notice.repository.NoticeRepository;
import com.quitmate.notice.repository.response.NoticeDto;
import com.quitmate.notice.service.NoticeReadService;
import com.quitmate.notice.service.request.NoticeListServiceRequest;
import com.quitmate.notice.service.response.NoticeListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeReadServiceImpl implements NoticeReadService {

    private final NoticeRepository noticeRepository;

    @Override
    public PageCustom<NoticeListResponse> getNoticeList(NoticeListServiceRequest request) {
        return PageCustom.of(noticeRepository.findNoticeList(request, request.toPageable()).map(NoticeListResponse::of));
    }
}
