package com.quitmate.challenge.challange.repository;

import com.quitmate.challenge.challange.repository.response.ChallengeDto;
import com.quitmate.challenge.challange.service.request.ChallengeSearchServiceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ChallengeRepository {
    Page<ChallengeDto> findChallengeList(ChallengeSearchServiceRequest request, Pageable pageable);
}
