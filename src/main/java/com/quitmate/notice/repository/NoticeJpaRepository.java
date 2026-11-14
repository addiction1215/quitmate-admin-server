package com.quitmate.notice.repository;

import com.quitmate.notice.entity.Notice;
import com.quitmate.notice.enums.NoticePushStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NoticeJpaRepository extends JpaRepository<Notice, Long> {

    @Query("select n from Notice n where n.useYn = 'Y'")
    List<Notice> findAll();

    @Query("select n from Notice n where n.id = :id and n.useYn = 'Y'")
    Optional<Notice> findById(@Param("id") Long id);

    @Query("select n.id from Notice n"
            + " where n.pushStatus = :status and n.pushScheduledAt <= :until and n.useYn = 'Y'"
            + " order by n.pushScheduledAt asc, n.id asc")
    List<Long> findIdsByPushStatusAndScheduledUntil(@Param("status") NoticePushStatus status,
                                                    @Param("until") LocalDateTime until);

    @Query("select n.id from Notice n where n.pushStatus = :status and n.pushSentAt < :startedBefore")
    List<Long> findIdsByPushStatusAndStartedBefore(@Param("status") NoticePushStatus status,
                                                   @Param("startedBefore") LocalDateTime startedBefore);

    /**
     * 보류한 푸시를 한 번만 집는다. 영향 행이 1이면 이 서버가 보내고, 0이면 다른 서버가 먼저 집었거나 그사이 지워진 것이다.
     * MySQL(InnoDB)은 UPDATE 때 행을 잠그고 최신 커밋 값으로 조건을 다시 보므로, 여러 대가 동시에 불러도 1을 받는 쪽은 하나다.
     */
    @Transactional
    @Modifying
    @Query("update Notice n set n.pushStatus = :to, n.pushSentAt = :now"
            + " where n.id = :id and n.pushStatus = :from and n.pushScheduledAt <= :now and n.useYn = 'Y'")
    int claimScheduledPush(@Param("id") Long id,
                           @Param("from") NoticePushStatus from,
                           @Param("to") NoticePushStatus to,
                           @Param("now") LocalDateTime now);

    /** 푸시 상태가 {@code from} 일 때만 {@code to} 로 바꾼다. 영향 행 수를 돌려준다 */
    @Transactional
    @Modifying
    @Query("update Notice n set n.pushStatus = :to where n.id = :id and n.pushStatus = :from")
    int updatePushStatus(@Param("id") Long id,
                         @Param("from") NoticePushStatus from,
                         @Param("to") NoticePushStatus to);
}
