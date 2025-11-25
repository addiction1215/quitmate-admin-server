package com.quitmate.inquiry.inquiryAnswer.controller;

import com.quitmate.global.ApiResponse;
import com.quitmate.inquiry.inquiryAnswer.controller.request.InquiryAnswerCreateRequest;
import com.quitmate.inquiry.inquiryAnswer.service.InquiryAnswerService;
import com.quitmate.inquiry.inquiryAnswer.service.response.InquiryAnswerCreateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/inquiry-answer")
public class InquiryAnswerController {

    private final InquiryAnswerService inquiryAnswerService;

    /**
     * 답변 등록 API
     */
    @PostMapping
    public ApiResponse<InquiryAnswerCreateResponse> createInquiryAnswer(
            @Valid @RequestBody InquiryAnswerCreateRequest request) {
        return ApiResponse.created(inquiryAnswerService.createInquiryAnswer(request.toServiceRequest()));
    }
}
