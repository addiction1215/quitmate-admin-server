package com.quitmate.challenge.missionhistory.service;

import com.quitmate.challenge.missionhistory.entity.MissionHistory;
import com.quitmate.challenge.missionhistory.repository.MissionHistoryRepository;
import com.quitmate.challenge.missionhistory.service.request.MissionHistoryListServiceRequest;
import com.quitmate.challenge.missionhistory.service.response.MissionHistoryDetailResponse;
import com.quitmate.challenge.missionhistory.service.response.MissionHistoryListResponse;
import com.quitmate.global.exception.QuitmateException;
import com.quitmate.global.page.response.PageCustom;
import com.quitmate.global.security.AdminAccessService;
import com.quitmate.locationAccessLog.enums.LocationAccessResourceType;
import com.quitmate.locationAccessLog.service.LocationAccessLogService;
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
    private final AdminAccessService adminAccessService;
    private final LocationAccessLogService locationAccessLogService;

    public MissionHistory findById(Long id) {
        return missionHistoryRepository.findById(id)
                .orElseThrow(() -> new QuitmateException(UNKNOWN_MISSION_HISTORY));
    }

    public PageCustom<MissionHistoryListResponse> getMissionHistoryList(
            MissionHistoryListServiceRequest request) {
        return PageCustom.of(missionHistoryRepository.findMissionHistoryList(request, request.toPageable())
                .map(MissionHistoryListResponse::createResponse));
    }

    /**
     * 미션 이력 상세. LOCATION 유형이면 회원의 GPS 좌표(locationData.addresses)가 나가므로,
     * 좌표가 하나라도 담기면 위치정보 열람 이력을 남긴다(KAN-208). 이력 저장이 실패해도 응답은 그대로 준다.
     */
    public MissionHistoryDetailResponse getMissionHistoryDetail(Long id) {
        // 누가 보는지 알아야 위치를 준다(KAN-206 인증 정보의 userId + DB role 확인)
        Long adminId = adminAccessService.getCurrentAdminId();
        MissionHistory missionHistory = missionHistoryRepository.findByIdWithAddresses(id)
                .orElseThrow(() -> new QuitmateException(UNKNOWN_MISSION_HISTORY));
        String photoUrl1 = toPresignedUrl(missionHistory.getPhotoUrl1());
        String photoUrl2 = toPresignedUrl(missionHistory.getPhotoUrl2());
        String photoUrl3 = toPresignedUrl(missionHistory.getPhotoUrl3());
        MissionHistoryDetailResponse response =
                MissionHistoryDetailResponse.createResponse(missionHistory, photoUrl1, photoUrl2, photoUrl3);
        if (response.containsLocation()) {
            locationAccessLogService.record(adminId, missionHistory.getUser().getId(),
                    LocationAccessResourceType.MISSION_HISTORY_ADDRESS, missionHistory.getId());
        }
        return response;
    }

    private String toPresignedUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        return s3StorageService.createPresignedUrl(objectKey, BucketKind.CHALLENGE);
    }
}
