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

    @Override
    public List<Mission> findByChallengeId(Long challengeId) {
        return missionJpaRepository.findByChallengeId(challengeId);
    }

    @Override
    public void deleteByChallengeId(Long challengeId) {
        missionJpaRepository.deleteByChallengeId(challengeId);
    }
}
