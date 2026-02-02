package com.quitmate.alertSetting.service;


import com.quitmate.alertSetting.entity.AlertSetting;
import com.quitmate.user.users.entity.User;

public interface AlertSettingReadService {
    AlertSetting findByUserOrCreateDefault(User user);
}
