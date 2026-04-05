package com.quitmate.challenge.missionhistory.repository;

import com.quitmate.challenge.missionhistory.entity.MissionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MissionHistoryJpaRepository extends JpaRepository<MissionHistory, Long> {

    @Query("SELECT m FROM MissionHistory m LEFT JOIN FETCH m.addresses WHERE m.id = :id")
    Optional<MissionHistory> findByIdWithAddresses(@Param("id") Long id);
}
