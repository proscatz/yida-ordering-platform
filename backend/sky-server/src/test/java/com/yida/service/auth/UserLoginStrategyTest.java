package com.yida.service.auth;

import com.yida.dto.UserLoginDTO;
import com.yida.entity.User;
import com.yida.exception.LoginFailedException;
import com.yida.mapper.UserMapper;
import com.yida.security.PasswordHasher;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserLoginStrategyTest {
    @Test void passwordLoginAcceptsUsernameOrPhoneWithBcrypt(){
        UserMapper mapper=mock(UserMapper.class);PasswordHasher hasher=new PasswordHasher();
        User user=User.builder().id(7L).password(hasher.encode("secret")).build();when(mapper.getByUsernameOrPhone("13800000000")).thenReturn(user);
        UserLoginDTO dto=new UserLoginDTO();dto.setPhone("13800000000");dto.setPassword("secret");
        assertSame(user,new PasswordUserLoginStrategy(mapper,hasher).login(dto));
    }
    @Test void passwordLoginRejectsInvalidPassword(){
        UserMapper mapper=mock(UserMapper.class);PasswordHasher hasher=new PasswordHasher();
        when(mapper.getByUsernameOrPhone("web-user")).thenReturn(User.builder().password(hasher.encode("secret")).build());
        UserLoginDTO dto=new UserLoginDTO();dto.setUsername("web-user");dto.setPassword("wrong");
        assertThrows(LoginFailedException.class,()->new PasswordUserLoginStrategy(mapper,hasher).login(dto));
    }
    @Test void wechatStrategyKeepsProviderSpecificExchangeIsolated(){
        WeChatLoginClient client=mock(WeChatLoginClient.class);UserMapper mapper=mock(UserMapper.class);UserLoginDTO dto=new UserLoginDTO();dto.setCode("code");
        when(client.exchangeCode("code")).thenReturn("openid-1");when(mapper.getByOpenid("openid-1")).thenReturn(null);
        User user=new WeChatUserLoginStrategy(client,mapper).login(dto);assertEquals("openid-1",user.getOpenid());verify(mapper).insert(user);
    }
}