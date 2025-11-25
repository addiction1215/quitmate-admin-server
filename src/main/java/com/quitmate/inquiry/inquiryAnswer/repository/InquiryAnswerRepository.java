package com.quitmate.inquiry.inquiryAnswer.repository;

import com.quitmate.inquiry.inquiryAnswer.entity.InquiryAnswer;

import java.util.Optional;

public interface InquiryAnswerRepository {
    Optional<InquiryAnswer> findById(Long id);

    InquiryAnswer save(InquiryAnswer inquiryAnswer);

    Optional<InquiryAnswer> findByInquiryQuestionId(Long inquiryQuestionId);
}
