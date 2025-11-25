package com.quitmate.notice.repository;

import com.quitmate.notice.entity.Notice;
import com.quitmate.notice.repository.response.NoticeDto;
import com.quitmate.notice.service.request.NoticeListServiceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface NoticeRepository {
    
    List<Notice> findAll();
    
    Notice save(Notice notice);
    
    Optional<Notice> findById(Long id);
    
    void deleteAllInBatch();
    
    void saveAll(List<Notice> notices);
    
    Page<NoticeDto> findNoticeList(NoticeListServiceRequest request, Pageable pageable);
}
