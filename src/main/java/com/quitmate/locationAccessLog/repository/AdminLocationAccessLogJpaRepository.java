package com.quitmate.locationAccessLog.repository;

import com.quitmate.locationAccessLog.entity.AdminLocationAccessLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AdminLocationAccessLogJpaRepository extends JpaRepository<AdminLocationAccessLog, Long> {

    /** accessed_at 인덱스를 타는 오래된 순 id 목록(보관 기간 삭제용) */
    @Query("select l.id from AdminLocationAccessLog l where l.accessedAt < :cutoff order by l.accessedAt asc, l.id asc")
    List<Long> findIdsAccessedBefore(@Param("cutoff") LocalDateTime cutoff, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from AdminLocationAccessLog l where l.id in :ids")
    int deleteByIdIn(@Param("ids") Collection<Long> ids);
}
