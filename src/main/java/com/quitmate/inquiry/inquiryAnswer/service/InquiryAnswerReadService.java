package com.quitmate.inquiry.inquiryAnswer.service;

import com.quitmate.inquiry.inquiryAnswer.entity.InquiryAnswer;

import java.util.Optional;

public interface InquiryAnswerReadService {
    InquiryAnswer findById(Long id);

    Optional<InquiryAnswer> findOptionalByInquiryQuestionId(Long questionId);
}
