package com.quitmate.user.users.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.quitmate.user.users.entity.User;

public interface UserJpaRepository extends JpaRepository<User, Long> {

    @Query("select u from User u where u.useYn = 'Y'")
    List<User> findAll();

	@Query("select u from User u where u.email = :email and u.useYn = 'Y'")
	Optional<User> findByEmail(@Param("email") String email);

	@Query("select u from User u where u.email = :email and u.nickName = :nickName and u.useYn = 'Y'")
	Optional<User> findByEmailAndNickName(@Param("email") String email, @Param("nickName") String nickName);

	@Query("select u from User u where u.id = :id and u.useYn = 'Y'")
	Optional<User> findById(@Param("id") Long id);

    boolean existsByEmail(String email);

}
