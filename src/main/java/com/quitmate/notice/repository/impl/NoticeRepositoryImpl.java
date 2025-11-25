package com.quitmate.notice.repository.impl;

import com.quitmate.notice.entity.Notice;
import com.quitmate.notice.repository.NoticeJpaRepository;
import com.quitmate.notice.repository.NoticeQueryRepository;
import com.quitmate.notice.repository.NoticeRepository;
import com.quitmate.notice.repository.response.NoticeDto;
import com.quitmate.notice.service.request.NoticeListServiceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NoticeRepositoryImpl implements NoticeRepository {

    private final NoticeRepository noticeRepository;

    @Override
    public List<Notice> findAll() {
        return noticeRepository.findAll();
    }

    @Override
    public Notice save(Notice notice) {
        return noticeRepository.save(notice);
    }

    @Override
    public Optional<Notice> findById(Long id) {
        return noticeRepository.findById(id);
    }

    @Override
    public void deleteAllInBatch() {
        noticeRepository.deleteAllInBatch();
    }

    @Override
    public void saveAll(List<Notice> notices) {
        noticeRepository.saveAll(notices);
    }

    @Override
    public Page<NoticeDto> findNoticeList(NoticeListServiceRequest request, Pageable pageable) {
        return noticeRepository.findNoticeList(request, pageable);
    }
}
