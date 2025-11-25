package com.quitmate.inquiry.inquiryAnswer.service.impl;

import com.quitmate.global.exception.QuitmateException;
import com.quitmate.inquiry.inquiryAnswer.entity.InquiryAnswer;
import com.quitmate.inquiry.inquiryAnswer.repository.InquiryAnswerRepository;
import com.quitmate.inquiry.inquiryAnswer.service.InquiryAnswerService;
import com.quitmate.inquiry.inquiryAnswer.service.request.InquiryAnswerCreateServiceRequest;
import com.quitmate.inquiry.inquiryAnswer.service.response.InquiryAnswerCreateResponse;
import com.quitmate.inquiry.inquryQuestion.entity.InquiryQuestion;
import com.quitmate.inquiry.inquryQuestion.enums.InquiryStatus;
import com.quitmate.inquiry.inquryQuestion.repository.InquiryQuestionJpaRepository;
import com.quitmate.inquiry.inquryQuestion.repository.InquiryQuestionRepository;
import com.quitmate.inquiry.inquryQuestion.service.InquiryQuestionReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InquiryAnswerServiceImpl implements InquiryAnswerService {

    private static final String UNKNOWN_INQUIRY_QUESTION = "해당 문의사항은 존재하지 않습니다.";
    private static final String ALREADY_ANSWERED = "이미 답변이 등록된 문의사항입니다.";

    private final InquiryQuestionReadService inquiryQuestionReadService;
    private final InquiryAnswerRepository inquiryAnswerRepository;

    @Override
    public InquiryAnswerCreateResponse createInquiryAnswer(InquiryAnswerCreateServiceRequest request) {
        // 1. 문의사항 존재 확인
        InquiryQuestion inquiryQuestion = inquiryQuestionReadService.findById(request.getInquiryQuestionId());

        // 2. 이미 답변이 있는지 확인
        inquiryAnswerRepository.findByInquiryQuestionId(request.getInquiryQuestionId())
                .ifPresent(inquiryAnswer -> {
                    throw new QuitmateException(ALREADY_ANSWERED);
                });

        // 3. 답변 생성
        InquiryAnswer inquiryAnswer = InquiryAnswer.builder()
                .inquiryQuestion(inquiryQuestion)
                .content(request.getContent())
                .build();

        InquiryAnswer savedInquiryAnswer = inquiryAnswerRepository.save(inquiryAnswer);

        // 4. 문의사항 상태를 '완료'로 변경
        inquiryQuestion.updateStatus(InquiryStatus.DONE);

        return InquiryAnswerCreateResponse.createResponse(savedInquiryAnswer);
    }
}
