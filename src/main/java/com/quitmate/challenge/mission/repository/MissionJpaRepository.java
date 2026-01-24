package com.quitmate.challenge.mission.repository;

import com.quitmate.challenge.mission.entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionJpaRepository extends JpaRepository<Mission, Long> {
}
