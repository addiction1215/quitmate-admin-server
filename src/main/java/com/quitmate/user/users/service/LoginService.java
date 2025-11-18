package com.quitmate.user.users.service;

import com.quitmate.user.users.service.request.LoginServiceRequest;
import com.quitmate.user.users.service.response.LoginResponse;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface LoginService {

	LoginResponse normalLogin(LoginServiceRequest loginServiceRequest) throws JsonProcessingException;
}
