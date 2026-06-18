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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ChallengeServiceImpl implements ChallengeService {

    private static final String UNKNOWN_MISSION = "해당 미션은 존재하지 않습니다.";

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
        challenge.update(request.getTitle(), request.getBadge(), request.getContent(), request.getReward());

        // 3. 기존 활성 미션 조회
        List<Mission> existingMissions = missionRepository.findByChallengeId(id);
        Map<Long, Mission> existingMissionMap = existingMissions.stream()
                .collect(Collectors.toMap(Mission::getId, mission -> mission));

        Set<Long> requestedMissionIds = new HashSet<>();
        List<Mission> newMissions = new ArrayList<>();

        // 4. 요청 미션 반영: 기존 미션은 수정, 신규 미션은 생성
        request.getMissions().forEach(missionRequest -> {
            Long missionId = missionRequest.getMissionId();

            if (missionId == null) {
                newMissions.add(missionRequest.toMissionEntity(challenge));
                return;
            }

            Mission mission = existingMissionMap.get(missionId);
            if (mission == null) {
                throw new IllegalArgumentException(UNKNOWN_MISSION);
            }

            mission.update(
                    missionRequest.getCategory(),
                    missionRequest.getTitle(),
                    missionRequest.getReward(),
                    missionRequest.getContent()
            );
            requestedMissionIds.add(missionId);
        });

        // 5. 요청에서 빠진 기존 미션은 이력 보존을 위해 soft delete
        existingMissions.stream()
                .filter(mission -> !requestedMissionIds.contains(mission.getId()))
                .forEach(Mission::delete);

        missionRepository.saveAll(newMissions);
    }

    @Override
    public void deleteChallenge(Long id) {
        // 1. 챌린지 존재 확인
        Challenge challenge = challengeReadService.findById(id);

        // 2. 이력 보존을 위해 활성 미션 soft delete
        missionRepository.findByChallengeId(id)
                .forEach(Mission::delete);

        // 3. 챌린지 soft delete
        challenge.delete();
    }
}
