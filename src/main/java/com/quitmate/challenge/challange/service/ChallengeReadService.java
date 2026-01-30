package com.quitmate.challenge.challange.service;

import com.quitmate.challenge.challange.entity.Challenge;
import com.quitmate.challenge.challange.repository.ChallengeRepository;
import com.quitmate.challenge.challange.repository.response.ChallengeDto;
import com.quitmate.challenge.challange.service.request.ChallengeSearchServiceRequest;
import com.quitmate.challenge.challange.service.response.ChallengeDetailResponse;
import com.quitmate.challenge.challange.service.response.ChallengeListResponse;
import com.quitmate.challenge.mission.entity.Mission;
import com.quitmate.challenge.mission.repository.MissionRepository;
import com.quitmate.global.exception.QuitmateException;
import com.quitmate.global.page.response.PageCustom;
import com.quitmate.global.page.response.PageableCustom;
import com.quitmate.storage.enums.BucketKind;
import com.quitmate.storage.service.S3StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChallengeReadService {

    private static final String UNKNOWN_CHALLENGE = "해당 챌린지는 존재하지 않습니다.";

    private final S3StorageService s3StorageService;

    private final ChallengeRepository challengeRepository;
    private final MissionRepository missionRepository;

    public Challenge findById(Long id) {
        return challengeRepository.findById(id)
                .orElseThrow(() -> new QuitmateException(UNKNOWN_CHALLENGE));
    }

    public PageCustom<ChallengeListResponse> getChallengeList(ChallengeSearchServiceRequest request) {
        Page<ChallengeDto> page = challengeRepository.findChallengeList(request, request.toPageable());

        List<ChallengeListResponse> challengeResponses = page.getContent().stream()
                .map(challenge -> ChallengeListResponse.createResponse(challenge, s3StorageService.createPresignedUrl(challenge.getBadge(), BucketKind.CHALLENGE_BADGE)))
                .toList();

        return PageCustom.<ChallengeListResponse>builder()
                .content(challengeResponses)
                .pageInfo(PageableCustom.of(page))
                .build();
    }

    public ChallengeDetailResponse getChallengeDetail(Long id) {
        Challenge challenge = findById(id);
        List<Mission> missions = missionRepository.findByChallengeId(id);

        return ChallengeDetailResponse.createResponse(challenge, missions, s3StorageService.createPresignedUrl(challenge.getBadge(), BucketKind.CHALLENGE_BADGE));
    }
}
