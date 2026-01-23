package com.quitmate.challenge.challange.repository.Impl;

import com.addiction.challenge.challange.controller.request.AdminChallengeSearchRequest;
import com.addiction.challenge.challange.entity.Challenge;
import com.addiction.challenge.challange.repository.ChallengeJpaRepository;
import com.addiction.challenge.challange.repository.ChallengeQueryRepository;
import com.addiction.challenge.challange.repository.ChallengeRepository;
import com.addiction.challenge.challange.repository.response.AdminChallengeDto;
import com.addiction.challenge.challange.repository.response.ChallengeDto;
import com.addiction.challenge.challengehistory.entity.ChallengeStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChallengeRepositoryImpl implements ChallengeRepository {
    private final ChallengeQueryRepository challengeQueryRepository;
    private final ChallengeJpaRepository challengeJpaRepository;

    @Override
    public List<ChallengeDto> findByUserId(Long userId) {
        return challengeQueryRepository.findByUserId(userId);
    }

    @Override
    public Challenge save(Challenge challenge) {
        return challengeJpaRepository.save(challenge);
    }

    @Override
    public Optional<ChallengeDto> findProgressingChallengeByUserId(Long userId) {
        return challengeQueryRepository.findProgressingChallengeByUserId(userId);
    }

    @Override
    public Page<ChallengeDto> findByUserIdAndStatus(Long userId, ChallengeStatus status, Pageable pageable) {
        return challengeQueryRepository.findByUserIdAndStatus(userId, status, pageable);
    }

    @Override
    public Optional<Challenge> findById(Long challengeId) {
        return challengeJpaRepository.findById(challengeId);
    }

    @Override
    public Page<AdminChallengeDto> findAllForAdmin(
            AdminChallengeSearchRequest.SearchCategory category,
            String keyword,
            Pageable pageable) {
        return challengeQueryRepository.findAllForAdmin(category, keyword, pageable);
    }
}
