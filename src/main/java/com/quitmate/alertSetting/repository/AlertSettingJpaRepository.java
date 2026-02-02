package com.quitmate.alertSetting.repository;

import com.quitmate.alertSetting.entity.AlertSetting;
import com.quitmate.user.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlertSettingJpaRepository extends JpaRepository<AlertSetting, Long> {

    Optional<AlertSetting> findByUser(User user);
}
