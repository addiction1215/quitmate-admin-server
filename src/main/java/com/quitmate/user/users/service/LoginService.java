package com.quitmate.user.users.service;

import com.quitmate.user.users.service.request.LoginServiceRequest;
import com.quitmate.user.users.service.request.OAuthLoginServiceRequest;
import com.quitmate.user.users.service.request.FindPasswordServiceRequest;
import com.quitmate.user.users.service.request.SendAuthCodeServiceRequest;
import com.quitmate.user.users.service.response.FindPasswordResponse;
import com.quitmate.user.users.service.response.LoginResponse;
import com.quitmate.user.users.service.response.OAuthLoginResponse;
import com.quitmate.user.users.service.response.SendAuthCodeResponse;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface LoginService {

	LoginResponse normalLogin(LoginServiceRequest loginServiceRequest) throws JsonProcessingException;
}
