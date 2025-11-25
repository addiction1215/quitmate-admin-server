package com.quitmate.inquiry.inquiryAnswer.repository;

import com.quitmate.inquiry.inquiryAnswer.entity.InquiryAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InquiryAnswerJpaRepository extends JpaRepository<InquiryAnswer, Long> {

    @Query("select ia from InquiryAnswer ia where ia.inquiryQuestion.id = :inquiryQuestionId")
    Optional<InquiryAnswer> findByInquiryQuestionId(@Param("inquiryQuestionId") Long inquiryQuestionId);
}
