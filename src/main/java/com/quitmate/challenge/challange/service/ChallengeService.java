package com.quitmate.challenge.challange.service;

import com.quitmate.challenge.challange.service.request.ChallengeCreateServiceRequest;
import com.quitmate.challenge.challange.service.response.ChallengeCreateResponse;

public interface ChallengeService {
    ChallengeCreateResponse createChallenge(ChallengeCreateServiceRequest request);
}
