package com.quitmate.alertSetting.repository.impl;

import com.quitmate.alertSetting.entity.AlertSetting;
import com.quitmate.alertSetting.repository.AlertSettingJpaRepository;
import com.quitmate.alertSetting.repository.AlertSettingRepository;
import com.quitmate.user.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AlertSettingRepositoryImpl implements AlertSettingRepository {

    private final AlertSettingJpaRepository alertSettingJpaRepository;

    @Override
    public AlertSetting save(AlertSetting alertSetting) {
        return alertSettingJpaRepository.save(alertSetting);
    }

    @Override
    public Optional<AlertSetting> findByUser(User user) {
        return alertSettingJpaRepository.findByUser(user);
    }

}
