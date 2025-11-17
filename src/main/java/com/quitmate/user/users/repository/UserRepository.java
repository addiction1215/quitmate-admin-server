package com.quitmate.user.users.repository;

import java.util.List;
import java.util.Optional;

import com.quitmate.user.users.entity.User;

public interface UserRepository {
    List<User> findAll();

	User save(User user);

	Optional<User> findByEmail(String email);

	Optional<User> findByEmailAndNickName(String email, String nickName);

	Optional<User> findById(Long id);

	void deleteAllInBatch();

	void saveAll(List<User> users);

    boolean existsByEmail(String email);
}
