package com.quitmate.user.userPass.repository;

import com.quitmate.user.userPass.entity.UserPass;
import com.quitmate.user.userPass.enums.UserPassProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserPassJpaRepository extends JpaRepository<UserPass, Long> {

    /** 회수가 엇갈리지 않도록 행을 잠근다. H2(테스트)·MySQL이 모두 받는 네이티브 FOR UPDATE */
    @Query(value = "select * from user_pass where id = :id for update", nativeQuery = true)
    Optional<UserPass> findByIdForUpdate(@Param("id") Long id);

    /**
     * 회수되지 않았고 기간 [startsAt, endsAt)과 겹치는 같은 상품 이용권.
     * 기간 끝은 포함하지 않으므로, 앞 이용권의 만료 시각에 시작하는 이용권은 겹치지 않는다.
     */
    @Query("select p from UserPass p where p.userId = :userId and p.product = :product and p.revokedAt is null "
            + "and p.startsAt < :endsAt and p.endsAt > :startsAt order by p.startsAt, p.id")
    List<UserPass> findOverlapping(@Param("userId") Long userId,
                                   @Param("product") UserPassProduct product,
                                   @Param("startsAt") LocalDateTime startsAt,
                                   @Param("endsAt") LocalDateTime endsAt);
}
