package com.quitmate.challenge.challange.repository;

import com.quitmate.challenge.challange.entity.Challenge;
import com.quitmate.challenge.challange.repository.response.ChallengeDto;
import com.quitmate.challenge.challange.service.request.ChallengeSearchServiceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ChallengeRepository {
    Page<ChallengeDto> findChallengeList(ChallengeSearchServiceRequest request, Pageable pageable);

    Challenge save(Challenge challenge);

    Optional<Challenge> findById(Long id);

    void deleteById(Long id);
}
