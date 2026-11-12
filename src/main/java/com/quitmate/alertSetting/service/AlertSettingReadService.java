package com.quitmate.alertSetting.service;


import com.quitmate.alertSetting.entity.AlertSetting;
import com.quitmate.user.users.entity.User;

import java.util.List;
import java.util.Map;

public interface AlertSettingReadService {
    AlertSetting findByUserOrCreateDefault(User user);

    /**
     * 사용자들의 알림 설정을 사용자 ID로 묶어 읽는다. 행이 없는 사용자는 결과에 없다.
     * 행을 새로 만들지 않는다(없으면 부르는 쪽이 앱 서버 기본값으로 본다).
     */
    Map<Long, AlertSetting> findAllByUsers(List<User> users);
}
