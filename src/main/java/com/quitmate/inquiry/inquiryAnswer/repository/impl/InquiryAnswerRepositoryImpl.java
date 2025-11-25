package com.quitmate.inquiry.inquiryAnswer.repository.impl;

import com.quitmate.inquiry.inquiryAnswer.entity.InquiryAnswer;
import com.quitmate.inquiry.inquiryAnswer.repository.InquiryAnswerJpaRepository;
import com.quitmate.inquiry.inquiryAnswer.repository.InquiryAnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class InquiryAnswerRepositoryImpl implements InquiryAnswerRepository {

    private final InquiryAnswerJpaRepository inquiryAnswerJpaRepository;

    @Override
    public Optional<InquiryAnswer> findById(Long id) {
        return inquiryAnswerJpaRepository.findById(id);
    }

    @Override
    public InquiryAnswer save(InquiryAnswer inquiryAnswer) {
        return inquiryAnswerJpaRepository.save(inquiryAnswer);
    }

    @Override
    public Optional<InquiryAnswer> findByInquiryQuestionId(Long inquiryQuestionId) {
        return inquiryAnswerJpaRepository.findByInquiryQuestionId(inquiryQuestionId);
    }
}
