package com.quitmate.notice.service.impl;

import com.quitmate.global.exception.QuitmateException;
import com.quitmate.notice.entity.Notice;
import com.quitmate.notice.repository.NoticeRepository;
import com.quitmate.notice.service.NoticeService;
import com.quitmate.notice.service.request.NoticeCreateServiceRequest;
import com.quitmate.notice.service.request.NoticeUpdateServiceRequest;
import com.quitmate.notice.service.response.NoticeCreateResponse;
import com.quitmate.notice.service.response.NoticeUpdateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NoticeServiceImpl implements NoticeService {

    private static final String UNKNOWN_NOTICE = "해당 공지사항은 존재하지 않습니다.";

    private final NoticeRepository noticeRepository;

    @Override
    public NoticeCreateResponse createNotice(NoticeCreateServiceRequest request) {
        return NoticeCreateResponse.of(noticeRepository.save(request.toEntity()));
    }

    @Override
    public NoticeUpdateResponse updateNotice(NoticeUpdateServiceRequest request) {
        Notice notice = noticeRepository.findById(request.getId())
                .orElseThrow(() -> new QuitmateException(UNKNOWN_NOTICE));

        notice.update(request.getType(), request.getContent());

        return NoticeUpdateResponse.of(notice);
    }
}
