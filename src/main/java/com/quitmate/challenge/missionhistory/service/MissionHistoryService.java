package com.quitmate.challenge.missionhistory.service;

import com.quitmate.challenge.missionhistory.entity.MissionHistory;
import com.quitmate.challenge.missionhistory.entity.MissionStatus;
import com.quitmate.challenge.missionhistory.service.request.MissionHistoryUpdateServiceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MissionHistoryService {

    private final MissionHistoryReadService missionHistoryReadService;

    public void updateMissionResult(Long id, MissionHistoryUpdateServiceRequest request) {
        MissionHistory missionHistory = missionHistoryReadService.findById(id);
        MissionStatus newStatus = request.getIsSuccess() ? MissionStatus.COMPLETED : MissionStatus.FAILED;
        missionHistory.updateStatus(newStatus);
    }
}
