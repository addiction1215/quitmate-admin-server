package com.quitmate.challenge.challange.repository;

import com.addiction.challenge.challange.controller.request.AdminChallengeSearchRequest;
import com.addiction.challenge.challange.entity.Challenge;
import com.addiction.challenge.challange.repository.response.AdminChallengeDto;
import com.addiction.challenge.challange.repository.response.ChallengeDto;
import com.addiction.challenge.challengehistory.entity.ChallengeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ChallengeRepository {
    List<ChallengeDto> findByUserId(Long userId);

    Challenge save(Challenge challenge);

    /**
     * 진행중인 챌린지 1개 조회 (최신순)
     */
    Optional<ChallengeDto> findProgressingChallengeByUserId(Long userId);

    /**
     * 특정 상태의 챌린지 목록 조회 (페이징)
     */
    Page<ChallengeDto> findByUserIdAndStatus(Long userId, ChallengeStatus status, Pageable pageable);

    Optional<Challenge> findById(Long challengeId);

    /**
     * Admin용 챌린지 목록 조회 (검색, 정렬, 페이징)
     */
    Page<AdminChallengeDto> findAllForAdmin(
            AdminChallengeSearchRequest.SearchCategory category,
            String keyword,
            Pageable pageable);
}
