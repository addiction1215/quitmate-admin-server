package com.quitmate.challenge.challange.service;

import com.addiction.challenge.challange.repository.ChallengeRepository;
import com.addiction.challenge.challange.repository.response.AdminChallengeDto;
import com.addiction.challenge.challange.service.request.AdminChallengeSearchServiceRequest;
import com.addiction.challenge.challange.service.response.AdminChallengeListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminChallengeService {
    
    private final ChallengeRepository challengeRepository;
    
    public Page<AdminChallengeListResponse> getChallengeList(AdminChallengeSearchServiceRequest request) {
        Page<AdminChallengeDto> challengePage = challengeRepository.findAllForAdmin(
                request.getCategory(),
                request.getKeyword(),
                request.getPageable()
        );
        
        return challengePage.map(this::toResponse);
    }
    
    private AdminChallengeListResponse toResponse(AdminChallengeDto dto) {
        return AdminChallengeListResponse.builder()
                .challengeId(dto.getChallengeId())
                .title(dto.getTitle())
                .badge(dto.getBadge())
                .reward(dto.getReward())
                .missionCount(dto.getMissionCount())
                .accumulatedCount(dto.getAccumulatedCount())
                .build();
    }
}
