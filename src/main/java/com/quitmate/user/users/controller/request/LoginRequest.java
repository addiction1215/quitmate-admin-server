package com.quitmate.user.users.controller.request;

import com.quitmate.user.users.service.request.LoginServiceRequest;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {

	@NotNull(message = "이메일은 필수입니다.")
	private String email;
	@NotNull(message = "비밀번호는 필수입니다.")
	private String password;

	@Builder
	private LoginRequest(String email, String password) {
		this.email = email;
		this.password = password;
	}

	public LoginServiceRequest toServiceRequest() {
		return LoginServiceRequest.builder()
			.email(email)
			.password(password)
			.build();
	}
}
