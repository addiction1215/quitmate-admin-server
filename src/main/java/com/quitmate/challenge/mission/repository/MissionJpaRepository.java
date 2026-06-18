package com.quitmate.challenge.mission.repository;

import com.quitmate.challenge.mission.entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MissionJpaRepository extends JpaRepository<Mission, Long> {
    @Query("select m from Mission m where m.challenge.id = :challengeId and m.useYn = 'Y'")
    List<Mission> findByChallengeId(@Param("challengeId") Long challengeId);

    void deleteByChallengeId(Long challengeId);
}
