package com.quitmate.inquiry.inquryQuestion.controller;

import com.quitmate.global.ApiResponse;
import com.quitmate.global.page.response.PageCustom;
import com.quitmate.inquiry.inquryQuestion.controller.request.InquiryQuestionListRequest;
import com.quitmate.inquiry.inquryQuestion.service.InquiryQuestionReadService;
import com.quitmate.inquiry.inquryQuestion.service.response.InquiryQuestionDetailResponse;
import com.quitmate.inquiry.inquryQuestion.service.response.InquiryQuestionListResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/inquiry-questions")
public class InquiryQuestionController {

    private final InquiryQuestionReadService inquiryQuestionReadService;

    /**
     * 문의사항 목록 조회 API
     */
    @GetMapping
    public ApiResponse<PageCustom<InquiryQuestionListResponse>> getInquiryQuestionList(
            @Valid @ModelAttribute InquiryQuestionListRequest request) {
        return ApiResponse.ok(inquiryQuestionReadService.getInquiryQuestionList(request.toServiceRequest()));
    }

    /**
     * 문의사항 상세 조회 API
     */
    @GetMapping("/{id}")
    public ApiResponse<InquiryQuestionDetailResponse> getInquiryQuestionDetail(
            @PathVariable Long id) {
        return ApiResponse.ok(inquiryQuestionReadService.getInquiryQuestionDetail(id));
    }
}
