package com.quitmate.user.users.service;

import com.quitmate.global.page.response.PageCustom;
import com.quitmate.user.users.entity.User;
import com.quitmate.user.users.service.request.UserListServiceRequest;
import com.quitmate.user.users.service.response.UserListResponse;

public interface UserReadService {

    User findByEmail(String email);

    User findById(Long id);

    PageCustom<UserListResponse> getUserList(UserListServiceRequest request);
}
