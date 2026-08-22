package com.yida.service.auth;

import com.yida.constant.MessageConstant;
import com.yida.dto.UserLoginDTO;
import com.yida.entity.User;
import com.yida.exception.LoginFailedException;
import com.yida.mapper.UserMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Component
@Order(2)
public class WeChatUserLoginStrategy implements UserLoginStrategy {
    private final WeChatLoginClient client;
    private final UserMapper userMapper;

    public WeChatUserLoginStrategy(WeChatLoginClient client, UserMapper userMapper) {
        this.client = client;
        this.userMapper = userMapper;
    }

    @Override
    public boolean supports(UserLoginDTO request) {
        return request != null && StringUtils.hasText(request.getCode());
    }

    @Override
    public User login(UserLoginDTO request) {
        String openid = client.exchangeCode(request.getCode().trim());
        if (!StringUtils.hasText(openid)) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        User user = userMapper.getByOpenid(openid);
        if (user == null) {
            user = User.builder().openid(openid).createTime(LocalDateTime.now()).build();
            userMapper.insert(user);
        }
        return user;
    }
}