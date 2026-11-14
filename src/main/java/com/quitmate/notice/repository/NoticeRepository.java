package com.quitmate.notice.repository;

import com.quitmate.notice.entity.Notice;
import com.quitmate.notice.enums.NoticePushStatus;
import com.quitmate.notice.repository.response.NoticeDto;
import com.quitmate.notice.service.request.NoticeListServiceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NoticeRepository {
    
    List<Notice> findAll();
    
    Notice save(Notice notice);
    
    Optional<Notice> findById(Long id);
    
    void deleteAllInBatch();
    
    void saveAll(List<Notice> notices);
    
    Page<NoticeDto> findNoticeList(NoticeListServiceRequest request, Pageable pageable);

    /** 푸시를 보류했고 예정 시각이 {@code now} 이전인, 지우지 않은 공지 ID. 예정 시각 순 */
    List<Long> findDeferredPushNoticeIdsDue(LocalDateTime now);

    /** 보내는 중(SENDING)으로 {@code startedBefore} 전부터 멈춰 있는 공지 ID */
    List<Long> findSendingPushNoticeIdsStartedBefore(LocalDateTime startedBefore);

    /** DEFERRED → SENDING 조건부 UPDATE. 1이면 이 서버가 집었다 */
    int claimDeferredPush(Long id, LocalDateTime now);

    /** 푸시 상태가 {@code from} 일 때만 {@code to} 로 바꾼다. 영향 행 수를 돌려준다 */
    int changePushStatus(Long id, NoticePushStatus from, NoticePushStatus to);
}
