package com.quitmate.challenge.missionhistory.service;

import com.quitmate.challenge.missionhistory.repository.MissionHistoryRepository;
import com.quitmate.challenge.missionhistory.service.request.MissionHistoryListServiceRequest;
import com.quitmate.challenge.missionhistory.service.response.MissionHistoryListResponse;
import com.quitmate.global.page.response.PageCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MissionHistoryReadService {

    private final MissionHistoryRepository missionHistoryRepository;

    public PageCustom<MissionHistoryListResponse> getMissionHistoryList(
            MissionHistoryListServiceRequest request) {
        return PageCustom.of(missionHistoryRepository.findMissionHistoryList(request, request.toPageable())
                .map(MissionHistoryListResponse::createResponse));
    }
}
