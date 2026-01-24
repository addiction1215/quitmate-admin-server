package com.quitmate.challenge.challange.service.impl;

import com.quitmate.challenge.challange.entity.Challenge;
import com.quitmate.challenge.challange.repository.ChallengeRepository;
import com.quitmate.challenge.challange.service.ChallengeReadService;
import com.quitmate.challenge.challange.service.ChallengeService;
import com.quitmate.challenge.challange.service.request.ChallengeCreateServiceRequest;
import com.quitmate.challenge.challange.service.request.ChallengeUpdateServiceRequest;
import com.quitmate.challenge.challange.service.response.ChallengeCreateResponse;
import com.quitmate.challenge.mission.entity.Mission;
import com.quitmate.challenge.mission.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ChallengeServiceImpl implements ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final MissionRepository missionRepository;
    private final ChallengeReadService challengeReadService;

    @Override
    public ChallengeCreateResponse createChallenge(ChallengeCreateServiceRequest request) {
        // 1. 챌린지 생성 및 저장
        Challenge savedChallenge = challengeRepository.save(request.toChallengeEntity());

        // 2. 미션들 생성 및 저장
        List<Mission> missions = request.getMissions().stream()
                .map(missionRequest -> missionRequest.toMissionEntity(savedChallenge))
                .collect(Collectors.toList());

        missionRepository.saveAll(missions);

        // 3. 응답 생성
        return ChallengeCreateResponse.createResponse(savedChallenge, missions.size());
    }

    @Override
    public void updateChallenge(Long id, ChallengeUpdateServiceRequest request) {
        // 1. 챌린지 존재 확인
        Challenge challenge = challengeReadService.findById(id);

        // 2. 챌린지 정보 업데이트
        challenge.update(request.getTitle(), request.getBadge(), request.getReward());

        // 3. 기존 미션 삭제
        missionRepository.deleteByChallengeId(id);

        // 4. 새로운 미션 생성 및 저장
        List<Mission> missions = request.getMissions().stream()
                .map(missionRequest -> missionRequest.toMissionEntity(challenge))
                .collect(Collectors.toList());

        missionRepository.saveAll(missions);
    }

    @Override
    public void deleteChallenge(Long id) {
        // 1. 챌린지 존재 확인
        Challenge challenge = challengeReadService.findById(id);

        // 2. 미션 삭제
        missionRepository.deleteByChallengeId(id);

        // 3. 챌린지 삭제
        challengeRepository.deleteById(id);
    }
}

