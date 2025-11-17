package com.quitmate.user.users.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.quitmate.global.ApiResponse;
import com.quitmate.user.users.controller.request.LoginRequest;
import com.quitmate.user.users.service.LoginService;
import com.quitmate.user.users.service.response.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class LoginController {

    private final LoginService loginService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) throws JsonProcessingException {
        return ApiResponse.ok(loginService.normalLogin(loginRequest.toServiceRequest()));
    }
}
