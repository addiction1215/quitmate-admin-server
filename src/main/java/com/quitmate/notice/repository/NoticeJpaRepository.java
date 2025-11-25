package com.quitmate.notice.repository;

import com.quitmate.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NoticeJpaRepository extends JpaRepository<Notice, Long> {

    @Query("select n from Notice n where n.useYn = 'Y'")
    List<Notice> findAll();

    @Query("select n from Notice n where n.id = :id and n.useYn = 'Y'")
    Optional<Notice> findById(@Param("id") Long id);
}
