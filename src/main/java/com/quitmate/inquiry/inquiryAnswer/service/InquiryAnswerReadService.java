package com.quitmate.inquiry.inquiryAnswer.service;

import com.quitmate.inquiry.inquiryAnswer.entity.InquiryAnswer;

public interface InquiryAnswerReadService {
    InquiryAnswer findById(Long id);

    InquiryAnswer findByInquiryQuestionId(Long questionId);
}
