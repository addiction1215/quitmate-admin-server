package com.quitmate.inquiry.inquryQuestion.repository;

import com.quitmate.inquiry.inquryQuestion.entity.InquiryQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InquiryQuestionJpaRepository extends JpaRepository<InquiryQuestion, Long> {

    @Query("select iq from InquiryQuestion iq join fetch iq.user where iq.id = :id")
    Optional<InquiryQuestion> findByIdWithUser(@Param("id") Long id);
}
