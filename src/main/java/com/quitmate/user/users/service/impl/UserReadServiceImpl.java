package com.quitmate.user.users.service.impl;

import com.quitmate.user.users.service.response.UserInfoResponse;
import com.quitmate.user.users.service.response.UserProfileResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quitmate.global.exception.QuitmateException;
import com.quitmate.global.security.SecurityService;
import com.quitmate.user.users.entity.User;
import com.quitmate.user.users.repository.UserRepository;
import com.quitmate.user.users.service.UserReadService;
import com.quitmate.user.users.service.response.UserPurposeResponse;
import com.quitmate.user.users.service.response.UserStartDateResponse;

import lombok.RequiredArgsConstructor;

import com.quitmate.user.users.service.response.UserSimpleProfileResponse;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserReadServiceImpl implements UserReadService {

	private static final String UNKNOWN_USER = "해당 회원은 존재하지 않습니다.";

	private final SecurityService securityService;

	private final UserRepository userRepository;

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
	public User findByEmail(String email) {
		return userRepository.findByEmail(email)
			.orElseThrow(() -> new QuitmateException(UNKNOWN_USER));
	}

	@Override
	public User findByEmailAndNickName(String email, String nickName) {
		return userRepository.findByEmailAndNickName(email, nickName)
			.orElseThrow(() -> new QuitmateException("입력하신 이메일과 닉네임에 해당하는 회원이 존재하지 않습니다."));
	}

	@Override
	public User findById(Long id) {
		return userRepository.findById(id)
			.orElseThrow(() -> new QuitmateException(UNKNOWN_USER));
	}

	@Override
	public UserStartDateResponse findStartDate() {
		return UserStartDateResponse.createResponse(
			findById(securityService.getCurrentLoginUserInfo().getUserId())
		);
	}

	@Override
	public UserPurposeResponse findPurpose() {
		return UserPurposeResponse.createResponse(
			findById(securityService.getCurrentLoginUserInfo().getUserId())
		);
	}

	@Override
	public UserProfileResponse findProfile() {
		return UserProfileResponse.createResponse(
				findById(securityService.getCurrentLoginUserInfo().getUserId())
		);
	}

	@Override
	public UserInfoResponse findUserInfo() {
		return UserInfoResponse.createResponse(
				findById(securityService.getCurrentLoginUserInfo().getUserId())
		);
	}

	@Override
	public UserSimpleProfileResponse findSimpleProfile() {
		return UserSimpleProfileResponse.createResponse(
			findById(securityService.getCurrentLoginUserInfo().getUserId())
		);
	}
}
