package com.quitmate.challenge.mission.repository.impl;

import com.quitmate.challenge.mission.entity.Mission;
import com.quitmate.challenge.mission.repository.MissionJpaRepository;
import com.quitmate.challenge.mission.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MissionRepositoryImpl implements MissionRepository {

    private final MissionJpaRepository missionJpaRepository;

    @Override
    public List<Mission> saveAll(List<Mission> missions) {
        return missionJpaRepository.saveAll(missions);
    }
}
