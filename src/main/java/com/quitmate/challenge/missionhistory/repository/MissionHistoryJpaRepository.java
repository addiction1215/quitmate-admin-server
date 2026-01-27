package com.quitmate.challenge.missionhistory.repository;

import com.quitmate.challenge.missionhistory.entity.MissionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionHistoryJpaRepository extends JpaRepository<MissionHistory, Long> {
}
