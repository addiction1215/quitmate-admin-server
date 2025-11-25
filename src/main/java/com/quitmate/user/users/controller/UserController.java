package com.quitmate.user.users.controller;

import com.quitmate.global.ApiResponse;
import com.quitmate.global.page.response.PageCustom;
import com.quitmate.user.users.controller.request.UserListRequest;
import com.quitmate.user.users.service.UserReadService;
import com.quitmate.user.users.service.response.UserListResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserReadService userReadService;

    @GetMapping
    public ApiResponse<PageCustom<UserListResponse>> getUserList(@ModelAttribute UserListRequest request) {
        return ApiResponse.ok(userReadService.getUserList(request.toServiceRequest()));
    }
}
