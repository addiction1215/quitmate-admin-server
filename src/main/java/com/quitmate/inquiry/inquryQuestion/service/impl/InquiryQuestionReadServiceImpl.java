package com.quitmate.inquiry.inquryQuestion.service.impl;

import com.quitmate.global.exception.QuitmateException;
import com.quitmate.global.page.response.PageCustom;
import com.quitmate.inquiry.inquiryAnswer.service.InquiryAnswerReadService;
import com.quitmate.inquiry.inquryQuestion.entity.InquiryQuestion;
import com.quitmate.inquiry.inquryQuestion.repository.InquiryQuestionRepository;
import com.quitmate.inquiry.inquryQuestion.service.InquiryQuestionReadService;
import com.quitmate.inquiry.inquryQuestion.service.request.InquiryQuestionListServiceRequest;
import com.quitmate.inquiry.inquryQuestion.service.response.InquiryQuestionDetailResponse;
import com.quitmate.inquiry.inquryQuestion.service.response.InquiryQuestionListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryQuestionReadServiceImpl implements InquiryQuestionReadService {

    private static final String UNKNOWN_INQUIRY_QUESTION = "해당 문의사항은 존재하지 않습니다.";

    private final InquiryAnswerReadService inquiryAnswerReadService;

    private final InquiryQuestionRepository inquiryQuestionRepository;

    @Override
    public InquiryQuestion findById(Long id) {
        return inquiryQuestionRepository.findById(id)
                .orElseThrow(() -> new QuitmateException(UNKNOWN_INQUIRY_QUESTION));
    }

    @Override
    public PageCustom<InquiryQuestionListResponse> getInquiryQuestionList(InquiryQuestionListServiceRequest request) {
        return PageCustom.of(inquiryQuestionRepository.findInquiryQuestionList(request, request.toPageable())
                .map(InquiryQuestionListResponse::createResponse));
    }

    @Override
    public InquiryQuestionDetailResponse getInquiryQuestionDetail(Long id) {
        return InquiryQuestionDetailResponse.createResponse(
                findById(id),
                inquiryAnswerReadService.findByInquiryQuestionId(id)
        );
    }
}
