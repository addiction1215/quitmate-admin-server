package com.quitmate.notice.controller;

import com.quitmate.global.ApiResponse;
import com.quitmate.global.page.response.PageCustom;
import com.quitmate.notice.controller.request.NoticeCreateRequest;
import com.quitmate.notice.controller.request.NoticeListRequest;
import com.quitmate.notice.controller.request.NoticeUpdateRequest;
import com.quitmate.notice.service.NoticeReadService;
import com.quitmate.notice.service.NoticeService;
import com.quitmate.notice.service.response.NoticeCreateResponse;
import com.quitmate.notice.service.response.NoticeDetailResponse;
import com.quitmate.notice.service.response.NoticeImageUploadResponse;
import com.quitmate.notice.service.response.NoticeListResponse;
import com.quitmate.notice.service.response.NoticeUpdateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @GetMapping("/{id}")
    public ApiResponse<NoticeDetailResponse> getNotice(@PathVariable Long id) {
        return ApiResponse.ok(noticeReadService.getNotice(id));
    }

    /**
     * 공지 본문 이미지 업로드. 응답의 key 를 본문 img 의 data-key 에, url 을 미리보기 src 에 쓴다.
     */
    @PostMapping("/image")
    public ApiResponse<NoticeImageUploadResponse> uploadNoticeImage(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(noticeService.uploadNoticeImage(file));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<NoticeCreateResponse> createNotice(
            @Valid @RequestBody NoticeCreateRequest request) {
        return ApiResponse.created(noticeService.createNotice(request.toServiceRequest()));
    }

    @PatchMapping
    public ApiResponse<NoticeUpdateResponse> updateNotice(
            @Valid @RequestBody NoticeUpdateRequest request) {
        return ApiResponse.ok(noticeService.updateNotice(request.toServiceRequest()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNotice(@PathVariable Long id) {
        noticeService.deleteNotice(id);
        return ApiResponse.ok(null);
    }
}
