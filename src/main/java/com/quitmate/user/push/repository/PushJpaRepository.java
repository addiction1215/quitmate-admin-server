package com.quitmate.user.push.repository;

import com.quitmate.user.push.entity.Push;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushJpaRepository extends JpaRepository<Push, Long> {
}
