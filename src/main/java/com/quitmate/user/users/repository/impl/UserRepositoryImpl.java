package com.quitmate.user.users.repository.impl;

import java.util.List;
import java.util.Optional;

import com.quitmate.user.users.repository.UserQueryRepository;
import com.quitmate.user.users.repository.response.UserDto;
import com.quitmate.user.users.service.request.UserListServiceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.quitmate.user.users.entity.User;
import com.quitmate.user.users.repository.UserJpaRepository;
import com.quitmate.user.users.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

	private final UserJpaRepository userJpaRepository;
	private final UserQueryRepository userQueryRepository;

    @Override
    public List<User> findAll() {
        return userJpaRepository.findAll();
    }

    @Override
	public User save(User user) {
		return userJpaRepository.save(user);
	}

	@Override
	public Optional<User> findByEmail(String email) {
		return userJpaRepository.findByEmail(email);
	}

	@Override
	public Optional<User> findByEmailAndNickName(String email, String nickName) {
		return userJpaRepository.findByEmailAndNickName(email, nickName);
	}

	@Override
	public Optional<User> findById(Long id) {
		return userJpaRepository.findById(id);
	}

	@Override
	public void deleteAllInBatch() {
		userJpaRepository.deleteAllInBatch();
	}

	@Override
	public void saveAll(List<User> users) {
		userJpaRepository.saveAll(users);
	}

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

	@Override
	public Page<UserDto> findUserList(UserListServiceRequest request, Pageable pageable) {
		return userQueryRepository.findUserList(request, pageable);
	}
}
