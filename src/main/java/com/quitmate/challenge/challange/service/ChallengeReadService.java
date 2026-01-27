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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChallengeReadService {

    private static final String UNKNOWN_CHALLENGE = "해당 챌린지는 존재하지 않습니다.";

    private final ChallengeRepository challengeRepository;
    private final MissionRepository missionRepository;

    public Challenge findById(Long id) {
        return challengeRepository.findById(id)
                .orElseThrow(() -> new QuitmateException(UNKNOWN_CHALLENGE));
    }

    public PageCustom<ChallengeListResponse> getChallengeList(ChallengeSearchServiceRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        Page<ChallengeDto> challengePage = challengeRepository.findChallengeList(request, pageable);

        return PageCustom.of(challengePage.map(this::toResponse));
    }

    public ChallengeDetailResponse getChallengeDetail(Long id) {
        Challenge challenge = findById(id);
        List<Mission> missions = missionRepository.findByChallengeId(id);

        return ChallengeDetailResponse.createResponse(challenge, missions);
    }

    private ChallengeListResponse toResponse(ChallengeDto dto) {
        return ChallengeListResponse.builder()
                .challengeId(dto.getChallengeId())
                .title(dto.getTitle())
                .badge(dto.getBadge())
                .reward(dto.getReward())
                .missionCount(dto.getMissionCount())
                .accumulatedCount(dto.getAccumulatedCount())
                .build();
    }
}
