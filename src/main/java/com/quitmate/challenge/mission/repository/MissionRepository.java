package com.quitmate.challenge.mission.repository;

import com.quitmate.challenge.mission.entity.Mission;

import java.util.List;

public interface MissionRepository {
    List<Mission> saveAll(List<Mission> missions);
}
