package com.quitmate.challenge.missionhistory.repository.impl;

import com.quitmate.challenge.missionhistory.repository.MissionHistoryJpaRepository;
import com.quitmate.challenge.missionhistory.repository.MissionHistoryQueryRepository;
import com.quitmate.challenge.missionhistory.repository.MissionHistoryRepository;
import com.quitmate.challenge.missionhistory.repository.response.MissionHistoryDto;
import com.quitmate.challenge.missionhistory.service.request.MissionHistoryListServiceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MissionHistoryRepositoryImpl implements MissionHistoryRepository {

    private final MissionHistoryJpaRepository missionHistoryJpaRepository;
    private final MissionHistoryQueryRepository missionHistoryQueryRepository;

    @Override
    public Page<MissionHistoryDto> findMissionHistoryList(
            MissionHistoryListServiceRequest request, Pageable pageable) {
        return missionHistoryQueryRepository.findCompletionRequestList(request, pageable);
    }
}
