package com.quitmate.challenge.challange.service;

import com.quitmate.challenge.challange.repository.ChallengeRepository;
import com.quitmate.challenge.challange.repository.response.ChallengeDto;
import com.quitmate.challenge.challange.service.request.ChallengeSearchServiceRequest;
import com.quitmate.challenge.challange.service.response.ChallengeListResponse;
import com.quitmate.global.page.response.PageCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChallengeReadService {

    private final ChallengeRepository challengeRepository;

    public PageCustom<ChallengeListResponse> getChallengeList(ChallengeSearchServiceRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        Page<ChallengeDto> challengePage = challengeRepository.findChallengeList(request, pageable);

        return PageCustom.of(challengePage.map(this::toResponse));
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
