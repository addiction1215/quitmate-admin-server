package com.quitmate.user.userPass.controller;

import com.quitmate.global.ApiResponse;
import com.quitmate.global.page.response.PageCustom;
import com.quitmate.user.userPass.controller.request.UserPassGrantRequest;
import com.quitmate.user.userPass.controller.request.UserPassListRequest;
import com.quitmate.user.userPass.controller.request.UserPassRevokeRequest;
import com.quitmate.user.userPass.service.UserPassService;
import com.quitmate.user.userPass.service.response.UserPassResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * QuitMate 90 이용권 관리(KAN-147). 관리자 계정만 쓸 수 있다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/user-passes")
public class UserPassController {

    private final UserPassService userPassService;

    @GetMapping
    public ApiResponse<PageCustom<UserPassResponse>> getUserPasses(@Valid @ModelAttribute UserPassListRequest request) {
        return ApiResponse.ok(userPassService.getUserPasses(request.toServiceRequest()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserPassResponse> grant(@Valid @RequestBody UserPassGrantRequest request) {
        return ApiResponse.created(userPassService.grant(request.toServiceRequest()));
    }

    @PatchMapping("/{passId}/revoke")
    public ApiResponse<UserPassResponse> revoke(@PathVariable Long passId,
                                                @Valid @RequestBody(required = false) UserPassRevokeRequest request) {
        UserPassRevokeRequest body = request == null ? UserPassRevokeRequest.builder().build() : request;
        return ApiResponse.ok(userPassService.revoke(body.toServiceRequest(passId)));
    }
}
