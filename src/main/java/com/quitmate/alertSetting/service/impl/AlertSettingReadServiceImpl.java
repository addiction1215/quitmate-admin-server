package com.quitmate.alertSetting.service.impl;

import com.quitmate.alertSetting.entity.AlertSetting;
import com.quitmate.alertSetting.repository.AlertSettingRepository;
import com.quitmate.alertSetting.service.AlertSettingReadService;
import com.quitmate.user.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AlertSettingReadServiceImpl implements AlertSettingReadService {

    private final AlertSettingRepository alertSettingRepository;

    @Override
    public AlertSetting findByUserOrCreateDefault(User user) {
        return alertSettingRepository.findByUser(user)
                .orElseGet(() -> alertSettingRepository.save(AlertSetting.createDefault(user)));
    }

}
