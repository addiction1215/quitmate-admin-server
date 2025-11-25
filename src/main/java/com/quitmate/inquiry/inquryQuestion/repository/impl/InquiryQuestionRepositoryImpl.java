package com.quitmate.inquiry.inquryQuestion.repository.impl;

import com.quitmate.inquiry.inquryQuestion.entity.InquiryQuestion;
import com.quitmate.inquiry.inquryQuestion.repository.InquiryQuestionJpaRepository;
import com.quitmate.inquiry.inquryQuestion.repository.InquiryQuestionQueryRepository;
import com.quitmate.inquiry.inquryQuestion.repository.InquiryQuestionRepository;
import com.quitmate.inquiry.inquryQuestion.repository.response.InquiryQuestionDto;
import com.quitmate.inquiry.inquryQuestion.service.request.InquiryQuestionListServiceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class InquiryQuestionRepositoryImpl implements InquiryQuestionRepository {

    private final InquiryQuestionJpaRepository inquiryQuestionJpaRepository;
    private final InquiryQuestionQueryRepository inquiryQuestionQueryRepository;

    @Override
    public Optional<InquiryQuestion> findById(Long id) {
        return inquiryQuestionJpaRepository.findById(id);
    }

    @Override
    public Page<InquiryQuestionDto> findInquiryQuestionList(InquiryQuestionListServiceRequest request, Pageable pageable) {
        return inquiryQuestionQueryRepository.findInquiryQuestionList(request, pageable);
    }
}
