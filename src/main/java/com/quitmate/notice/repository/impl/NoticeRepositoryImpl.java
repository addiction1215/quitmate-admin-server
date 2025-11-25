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


    private final NoticeJpaRepository noticeJpaRepository;

    private final NoticeQueryRepository noticeQueryRepository;

    @Override
    public List<Notice> findAll() {
        return noticeJpaRepository.findAll();
    }

    @Override
    public Notice save(Notice notice) {

        return noticeJpaRepository.save(notice);
    }

    @Override
    public Optional<Notice> findById(Long id) {
        return noticeJpaRepository.findById(id);
    }

    @Override
    public void deleteAllInBatch() {
        noticeJpaRepository.deleteAllInBatch();
    }

    @Override
    public void saveAll(List<Notice> notices) {
        noticeJpaRepository.saveAll(notices);
    }

    @Override
    public Page<NoticeDto> findNoticeList(NoticeListServiceRequest request, Pageable pageable) {
        return noticeQueryRepository.findNoticeList(request, pageable);
    }
}
