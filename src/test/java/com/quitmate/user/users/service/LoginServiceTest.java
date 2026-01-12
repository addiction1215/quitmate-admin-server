package com.quitmate.user.users.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mysema.commons.lang.Assert;
import com.quitmate.IntegrationTestSupport;
import com.quitmate.global.exception.QuitmateException;
import com.quitmate.user.users.entity.User;
import com.quitmate.user.users.entity.enums.SettingStatus;
import com.quitmate.user.users.entity.enums.SnsType;
import com.quitmate.user.users.service.request.LoginServiceRequest;
import com.quitmate.user.users.service.response.LoginResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

public class LoginServiceTest extends IntegrationTestSupport {

	@Autowired
	private LoginService loginService;

	@DisplayName("일반으로 등록된 사용자가 있을 경우 일반 로그인을 한다.")
	@Test
	void normalLoginIfUserExist() throws JsonProcessingException {
		// given
		User user = createUser("tkdrl8908@naver.com", "1234", SnsType.NORMAL, SettingStatus.INCOMPLETE);

		User savedUser = userRepository.save(user);

		LoginServiceRequest request = LoginServiceRequest.builder()
			.email(savedUser.getEmail())
			.password("1234")
			.build();

		// when
		LoginResponse loginResponse = loginService.normalLogin(request);

		// then
		assertThat(loginResponse.getEmail()).isEqualTo(savedUser.getEmail());
	}

	@DisplayName("등록된 사용자가 없을 경우 일반 로그인을 할 시 예외를 발생시킨다.")
	@Test
	void normalLoginIfUserNotExist() {
		// given
		LoginServiceRequest request = LoginServiceRequest.builder()
			.email("tkdrl8908@naver.com")
			.password("1234")
			.build();

		// when
		// then
		assertThrows(QuitmateException.class, () -> {
			loginService.normalLogin(request);
		});
	}

	@DisplayName("일반 로그인시 비밀번호가 틀렸을 경우 예외를 발생시킨다.")
	@Test
	void normalLoginIncorrectPassword() {
		// given
		User user = createUser("tkdrl8908@naver.com", "1234", SnsType.NORMAL, SettingStatus.INCOMPLETE);

		userRepository.save(user);

		LoginServiceRequest request = LoginServiceRequest.builder()
			.email("tkdrl8908@naver.com")
			.password("12345678")
			.build();

		// when
		// then
		assertThrows(QuitmateException.class, () -> {
			loginService.normalLogin(request);
		});
	}
}
