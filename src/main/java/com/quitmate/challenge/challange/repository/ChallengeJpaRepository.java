package com.quitmate.challenge.challange.repository;

import com.quitmate.challenge.challange.entity.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChallengeJpaRepository extends JpaRepository<Challenge, Long> {
}
