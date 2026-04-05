package com.quitmate.challenge.missionhistory.service;

import com.quitmate.challenge.missionhistory.entity.MissionHistory;
import com.quitmate.challenge.missionhistory.repository.MissionHistoryRepository;
import com.quitmate.challenge.missionhistory.service.request.MissionHistoryListServiceRequest;
import com.quitmate.challenge.missionhistory.service.response.MissionHistoryDetailResponse;
import com.quitmate.challenge.missionhistory.service.response.MissionHistoryListResponse;
import com.quitmate.global.exception.QuitmateException;
import com.quitmate.global.page.response.PageCustom;
import com.quitmate.storage.enums.BucketKind;
import com.quitmate.storage.service.S3StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MissionHistoryReadService {

    private static final String UNKNOWN_MISSION_HISTORY = "해당 미션 히스토리는 존재하지 않습니다.";

    private final MissionHistoryRepository missionHistoryRepository;
    private final S3StorageService s3StorageService;

    public MissionHistory findById(Long id) {
        return missionHistoryRepository.findById(id)
                .orElseThrow(() -> new QuitmateException(UNKNOWN_MISSION_HISTORY));
    }

    public PageCustom<MissionHistoryListResponse> getMissionHistoryList(
            MissionHistoryListServiceRequest request) {
        return PageCustom.of(missionHistoryRepository.findMissionHistoryList(request, request.toPageable())
                .map(MissionHistoryListResponse::createResponse));
    }

    public MissionHistoryDetailResponse getMissionHistoryDetail(Long id) {
        MissionHistory missionHistory = missionHistoryRepository.findByIdWithAddresses(id)
                .orElseThrow(() -> new QuitmateException(UNKNOWN_MISSION_HISTORY));
        String photoUrl1 = toPresignedUrl(missionHistory.getPhotoUrl1());
        String photoUrl2 = toPresignedUrl(missionHistory.getPhotoUrl2());
        String photoUrl3 = toPresignedUrl(missionHistory.getPhotoUrl3());
        return MissionHistoryDetailResponse.createResponse(missionHistory, photoUrl1, photoUrl2, photoUrl3);
    }

    private String toPresignedUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        return s3StorageService.createPresignedUrl(objectKey, BucketKind.CHALLENGE);
    }
}
