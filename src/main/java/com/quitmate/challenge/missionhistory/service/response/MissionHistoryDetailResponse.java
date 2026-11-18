package com.quitmate.challenge.missionhistory.service.response;

import com.quitmate.challenge.mission.entity.MissionCategoryStatus;
import com.quitmate.challenge.missionhistory.entity.MissionHistory;
import com.quitmate.challenge.missionhistory.entity.MissionStatus;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class MissionHistoryDetailResponse {
    private final Long missionHistoryId;
    private final String challengeTitle;
    private final String missionTitle;
    private final MissionCategoryStatus category;
    private final MissionStatus status;
    private final String userName;
    private final LocalDateTime createdDate;

    // 카테고리별 데이터
    private final LocationVerificationData locationData;
    private final PhotoVerificationData photoData;
    private final AbstinenceVerificationData abstinenceData;

    @Builder
    private MissionHistoryDetailResponse(Long missionHistoryId, String challengeTitle, String missionTitle,
                                         MissionCategoryStatus category, MissionStatus status, String userName,
                                         LocalDateTime createdDate, LocationVerificationData locationData,
                                         PhotoVerificationData photoData, AbstinenceVerificationData abstinenceData) {
        this.missionHistoryId = missionHistoryId;
        this.challengeTitle = challengeTitle;
        this.missionTitle = missionTitle;
        this.category = category;
        this.status = status;
        this.userName = userName;
        this.createdDate = createdDate;
        this.locationData = locationData;
        this.photoData = photoData;
        this.abstinenceData = abstinenceData;
    }

    public static MissionHistoryDetailResponse createResponse(MissionHistory missionHistory,
                                                               String photoUrl1, String photoUrl2, String photoUrl3) {
        MissionCategoryStatus category = missionHistory.getMission().getCategory();

        return MissionHistoryDetailResponse.builder()
                .missionHistoryId(missionHistory.getId())
                .challengeTitle(missionHistory.getMission().getChallenge().getTitle())
                .missionTitle(missionHistory.getMission().getTitle())
                .category(category)
                .status(missionHistory.getStatus())
                .userName(missionHistory.getUser().getNickName())
                .createdDate(missionHistory.getCreatedDate())
                .locationData(category == MissionCategoryStatus.LOCATION ?
                        LocationVerificationData.of(missionHistory.getGpsVerifyCount(), missionHistory.getAddresses()) : null)
                .photoData(category == MissionCategoryStatus.REPLACE_ACTION ?
                        PhotoVerificationData.of(photoUrl1, photoUrl2, photoUrl3) : null)
                .abstinenceData(category == MissionCategoryStatus.HOLD ?
                        AbstinenceVerificationData.of(missionHistory.getAbstinenceTime()) : null)
                .build();
    }

    /**
     * 이 응답에 위치 값(locationData.addresses 의 좌표)이 하나라도 담겼는지. 위치정보 열람 이력(KAN-208)을 남길지 정한다.
     * LOCATION 이 아닌 유형, 반려로 좌표가 지워진 LOCATION 은 false 다.
     * 응답 JSON 칸이 아니다(get·is 로 시작하지 않아 Jackson 이 내보내지 않는다).
     */
    public boolean containsLocation() {
        return locationData != null && locationData.getAddresses() != null
                && locationData.getAddresses().stream().anyMatch(StringUtils::hasText);
    }

    @Getter
    public static class LocationVerificationData {
        private final Integer gpsVerifyCount;
        private final List<String> addresses;

        private LocationVerificationData(Integer gpsVerifyCount, List<String> addresses) {
            this.gpsVerifyCount = gpsVerifyCount;
            this.addresses = addresses;
        }

        public static LocationVerificationData of(Integer gpsVerifyCount, List<String> addresses) {
            return new LocationVerificationData(gpsVerifyCount, addresses);
        }
    }

    @Getter
    public static class PhotoVerificationData {
        private final String photoUrl1;
        private final String photoUrl2;
        private final String photoUrl3;

        private PhotoVerificationData(String photoUrl1, String photoUrl2, String photoUrl3) {
            this.photoUrl1 = photoUrl1;
            this.photoUrl2 = photoUrl2;
            this.photoUrl3 = photoUrl3;
        }

        public static PhotoVerificationData of(String photoUrl1, String photoUrl2, String photoUrl3) {
            return new PhotoVerificationData(photoUrl1, photoUrl2, photoUrl3);
        }
    }

    @Getter
    public static class AbstinenceVerificationData {
        private final Integer abstinenceTime;

        private AbstinenceVerificationData(Integer abstinenceTime) {
            this.abstinenceTime = abstinenceTime;
        }

        public static AbstinenceVerificationData of(Integer abstinenceTime) {
            return new AbstinenceVerificationData(abstinenceTime);
        }
    }
}
