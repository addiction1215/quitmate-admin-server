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

    /**
     * 같은 회원에게 동시에 이용권을 부여할 때 겹침 확인이 엇갈리지 않도록 회원 행을 잠근다(KAN-147).
     * JPA 비관적 락은 테스트 DB(H2, PostgreSQL 방언)에서 지원하지 않는 SQL이 되어, MySQL·H2가 모두 받는 네이티브 FOR UPDATE를 쓴다.
     */
	@Query(value = "select id from users where id = :id and use_yn = 'Y' for update", nativeQuery = true)
	Optional<Long> lockActiveUserId(@Param("id") Long id);

    boolean existsByEmail(String email);

	@Query("select distinct u from User u left join fetch u.pushes where u.useYn = 'Y'")
	List<User> findAllWithPushes();

}
