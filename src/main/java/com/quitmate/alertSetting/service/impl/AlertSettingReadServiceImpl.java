package com.quitmate.alertSetting.service.impl;

import com.quitmate.alertSetting.entity.AlertSetting;
import com.quitmate.alertSetting.repository.AlertSettingRepository;
import com.quitmate.alertSetting.service.AlertSettingReadService;
import com.quitmate.user.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AlertSettingReadServiceImpl implements AlertSettingReadService {

    private static final int IN_CLAUSE_CHUNK_SIZE = 1000;

    private final AlertSettingRepository alertSettingRepository;

    @Override
    public AlertSetting findByUserOrCreateDefault(User user) {
        return alertSettingRepository.findByUser(user)
                .orElseGet(() -> alertSettingRepository.save(AlertSetting.createDefault(user)));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, AlertSetting> findAllByUsers(List<User> users) {
        Map<Long, AlertSetting> settingsByUserId = new HashMap<>();
        // IN 절이 너무 길어지지 않게 나눠 읽는다.
        for (int from = 0; from < users.size(); from += IN_CLAUSE_CHUNK_SIZE) {
            List<User> chunk = users.subList(from, Math.min(from + IN_CLAUSE_CHUNK_SIZE, users.size()));
            alertSettingRepository.findAllByUserIn(chunk)
                    .forEach(setting -> settingsByUserId.put(setting.getUser().getId(), setting));
        }
        return settingsByUserId;
    }

}
