package com.quitmate.faq.controller;

import com.quitmate.faq.controller.request.FaqCreateRequest;
import com.quitmate.faq.controller.request.FaqUpdateRequest;
import com.quitmate.faq.service.FaqService;
import com.quitmate.faq.service.response.FaqCreateResponse;
import com.quitmate.faq.service.response.FaqUpdateResponse;
import com.quitmate.global.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/faq")
public class FaqController {

    private final FaqService faqService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FaqCreateResponse> createFaq(
            @Valid @RequestBody FaqCreateRequest request) {
        return ApiResponse.created(faqService.createFaq(request.toServiceRequest()));
    }

    @PatchMapping
    public ApiResponse<FaqUpdateResponse> updateFaq(
            @Valid @RequestBody FaqUpdateRequest request) {
        return ApiResponse.ok(faqService.updateFaq(request.toServiceRequest()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteFaq(@PathVariable Long id) {
        faqService.deleteFaq(id);
        return ApiResponse.ok(null);
    }
}
