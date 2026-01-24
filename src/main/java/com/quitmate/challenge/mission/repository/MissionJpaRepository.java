package com.quitmate.challenge.mission.repository;

import com.quitmate.challenge.mission.entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MissionJpaRepository extends JpaRepository<Mission, Long> {
    List<Mission> findByChallengeId(Long challengeId);

    void deleteByChallengeId(Long challengeId);
}
