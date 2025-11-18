package com.quitmate.user.users.service;

import com.quitmate.user.users.entity.User;
import com.quitmate.user.users.service.response.*;

public interface UserReadService {

    User findByEmail(String email);

    User findById(Long id);
}
