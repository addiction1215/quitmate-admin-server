package com.quitmate.inquiry.inquiryAnswer.service.impl;

import com.quitmate.inquiry.inquiryAnswer.entity.InquiryAnswer;
import com.quitmate.inquiry.inquiryAnswer.repository.InquiryAnswerRepository;
import com.quitmate.inquiry.inquiryAnswer.service.InquiryAnswerReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryAnswerReadServiceImpl implements InquiryAnswerReadService {

    private static final String INQUIRY_ANSWER_NOT_FOUND = "문의 답변이 존재하지 않습니다.";

    private final InquiryAnswerRepository inquiryAnswerRepository;

    @Override
    public InquiryAnswer findById(Long id) {
        return inquiryAnswerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(INQUIRY_ANSWER_NOT_FOUND));
    }

    @Override
    public InquiryAnswer findByInquiryQuestionId(Long questionId) {
        return inquiryAnswerRepository.findByInquiryQuestionId(questionId)
                .orElseThrow(() -> new IllegalArgumentException(INQUIRY_ANSWER_NOT_FOUND));
    }

}
