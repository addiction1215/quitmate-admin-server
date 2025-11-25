package com.quitmate.notice.controller;

import com.quitmate.global.ApiResponse;
import com.quitmate.global.page.response.PageCustom;
import com.quitmate.notice.controller.request.NoticeCreateRequest;
import com.quitmate.notice.controller.request.NoticeListRequest;
import com.quitmate.notice.controller.request.NoticeUpdateRequest;
import com.quitmate.notice.service.NoticeReadService;
import com.quitmate.notice.service.NoticeService;
import com.quitmate.notice.service.response.NoticeCreateResponse;
import com.quitmate.notice.service.response.NoticeListResponse;
import com.quitmate.notice.service.response.NoticeUpdateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notice")
public class NoticeController {

    private final NoticeReadService noticeReadService;
    private final NoticeService noticeService;

    @GetMapping
    public ApiResponse<PageCustom<NoticeListResponse>> getNoticeList(
            @Valid @ModelAttribute NoticeListRequest request) {
        return ApiResponse.ok(noticeReadService.getNoticeList(request.toServiceRequest()));
    }

    @PostMapping
    public ApiResponse<NoticeCreateResponse> createNotice(
            @Valid @RequestBody NoticeCreateRequest request) {
        return ApiResponse.created(noticeService.createNotice(request.toServiceRequest()));
    }

    @PatchMapping
    public ApiResponse<NoticeUpdateResponse> updateNotice(
            @Valid @RequestBody NoticeUpdateRequest request) {
        return ApiResponse.ok(noticeService.updateNotice(request.toServiceRequest()));
    }
}
