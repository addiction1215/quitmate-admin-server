package com.quitmate.challenge.challange.repository;

import com.quitmate.challenge.challange.entity.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChallengeJpaRepository extends JpaRepository<Challenge, Long> {

    @Query("select c from Challenge c where c.id = :id and c.useYn = 'Y'")
    Optional<Challenge> findById(@Param("id") Long id);
}
