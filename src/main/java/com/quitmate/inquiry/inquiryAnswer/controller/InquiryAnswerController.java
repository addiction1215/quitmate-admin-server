package com.quitmate.inquiry.inquiryAnswer.controller;

import com.quitmate.global.ApiResponse;
import com.quitmate.inquiry.inquiryAnswer.controller.request.InquiryAnswerCreateRequest;
import com.quitmate.inquiry.inquiryAnswer.service.InquiryAnswerService;
import com.quitmate.inquiry.inquiryAnswer.service.response.InquiryAnswerCreateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/inquiry-answer")
public class InquiryAnswerController {

    private final InquiryAnswerService inquiryAnswerService;

    /**
     * 답변 등록 API
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InquiryAnswerCreateResponse> createInquiryAnswer(
            @Valid @RequestBody InquiryAnswerCreateRequest request) {
        return ApiResponse.created(inquiryAnswerService.createInquiryAnswer(request.toServiceRequest()));
    }
}
