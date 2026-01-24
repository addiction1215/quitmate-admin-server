package com.quitmate.challenge.challange.repository.Impl;

import com.quitmate.challenge.challange.repository.ChallengeQueryRepository;
import com.quitmate.challenge.challange.repository.ChallengeRepository;
import com.quitmate.challenge.challange.repository.response.ChallengeDto;
import com.quitmate.challenge.challange.service.request.ChallengeSearchServiceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ChallengeRepositoryImpl implements ChallengeRepository {
    private final ChallengeQueryRepository challengeQueryRepository;

    @Override
    public Page<ChallengeDto> findChallengeList(ChallengeSearchServiceRequest request, Pageable pageable) {
        return challengeQueryRepository.findAllForAdmin(request, pageable);
    }
}
