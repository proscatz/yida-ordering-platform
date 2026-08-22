package com.yida.service.impl;

import com.yida.constant.MessageConstant;
import com.yida.dto.UserLoginDTO;
import com.yida.entity.User;
import com.yida.exception.LoginFailedException;
import com.yida.service.UserService;
import com.yida.service.auth.UserLoginStrategy;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    private final List<UserLoginStrategy> strategies;

    public UserServiceImpl(List<UserLoginStrategy> strategies) { this.strategies = strategies; }

    @Override
    public User login(UserLoginDTO request) {
        return strategies.stream().filter(strategy -> strategy.supports(request)).findFirst()
                .orElseThrow(() -> new LoginFailedException(MessageConstant.LOGIN_FAILED))
                .login(request);
    }
}