package com.quitmate.challenge.missionhistory.repository.impl;

import com.quitmate.challenge.missionhistory.entity.MissionHistory;
import com.quitmate.challenge.missionhistory.repository.MissionHistoryJpaRepository;
import com.quitmate.challenge.missionhistory.repository.MissionHistoryQueryRepository;
import com.quitmate.challenge.missionhistory.repository.MissionHistoryRepository;
import com.quitmate.challenge.missionhistory.repository.response.MissionHistoryDto;
import com.quitmate.challenge.missionhistory.service.request.MissionHistoryListServiceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MissionHistoryRepositoryImpl implements MissionHistoryRepository {

    private final MissionHistoryJpaRepository missionHistoryJpaRepository;
    private final MissionHistoryQueryRepository missionHistoryQueryRepository;

    @Override
    public Page<MissionHistoryDto> findMissionHistoryList(
            MissionHistoryListServiceRequest request, Pageable pageable) {
        return missionHistoryQueryRepository.findMissionHistoryList(request, pageable);
    }

    @Override
    public Optional<MissionHistory> findById(Long id) {
        return missionHistoryJpaRepository.findById(id);
    }
}
