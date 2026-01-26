package com.quitmate.challenge.missionhistory.repository;

import com.quitmate.challenge.missionhistory.entity.MissionHistory;
import com.quitmate.challenge.missionhistory.repository.response.MissionHistoryDto;
import com.quitmate.challenge.missionhistory.service.request.MissionHistoryListServiceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface MissionHistoryRepository {
    Page<MissionHistoryDto> findMissionHistoryList(MissionHistoryListServiceRequest request, Pageable pageable);

    Optional<MissionHistory> findById(Long id);
}
