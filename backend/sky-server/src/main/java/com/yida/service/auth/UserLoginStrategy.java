package com.yida.service.auth;

import com.yida.dto.UserLoginDTO;
import com.yida.entity.User;

public interface UserLoginStrategy {
    boolean supports(UserLoginDTO request);
    User login(UserLoginDTO request);
}