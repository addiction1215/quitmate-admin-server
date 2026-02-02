package com.quitmate.alertSetting.repository;


import com.quitmate.alertSetting.entity.AlertSetting;
import com.quitmate.user.users.entity.User;

import java.util.Optional;

public interface AlertSettingRepository {
    AlertSetting save(AlertSetting alertSetting);

    Optional<AlertSetting> findByUser(User user);
}
