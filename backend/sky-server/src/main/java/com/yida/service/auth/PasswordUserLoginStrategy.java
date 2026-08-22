package com.yida.service.auth;

import com.yida.constant.MessageConstant;
import com.yida.dto.UserLoginDTO;
import com.yida.entity.User;
import com.yida.exception.LoginFailedException;
import com.yida.mapper.UserMapper;
import com.yida.security.PasswordHasher;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Order(1)
public class PasswordUserLoginStrategy implements UserLoginStrategy {
    private final UserMapper userMapper;
    private final PasswordHasher passwordHasher;

    public PasswordUserLoginStrategy(UserMapper userMapper, PasswordHasher passwordHasher) {
        this.userMapper = userMapper;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public boolean supports(UserLoginDTO request) {
        return request != null && StringUtils.hasText(request.getPassword())
                && (StringUtils.hasText(request.getUsername()) || StringUtils.hasText(request.getPhone()));
    }

    @Override
    public User login(UserLoginDTO request) {
        String identifier = StringUtils.hasText(request.getUsername())
                ? request.getUsername().trim() : request.getPhone().trim();
        User user = userMapper.getByUsernameOrPhone(identifier);
        if (user == null || !passwordHasher.matchesBcrypt(request.getPassword(), user.getPassword())) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        return user;
    }
}