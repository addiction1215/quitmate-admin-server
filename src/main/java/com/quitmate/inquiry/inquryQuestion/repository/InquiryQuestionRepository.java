package com.quitmate.inquiry.inquryQuestion.repository;

import com.quitmate.inquiry.inquryQuestion.entity.InquiryQuestion;
import com.quitmate.inquiry.inquryQuestion.repository.response.InquiryQuestionDto;
import com.quitmate.inquiry.inquryQuestion.service.request.InquiryQuestionListServiceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface InquiryQuestionRepository {
    Optional<InquiryQuestion> findById(Long id);
    
    Page<InquiryQuestionDto> findInquiryQuestionList(InquiryQuestionListServiceRequest request, Pageable pageable);
}
