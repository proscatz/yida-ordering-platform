package com.yida.controller.user;

import com.yida.dto.UserLoginDTO;
import com.yida.entity.User;
import com.yida.properties.JwtProperties;
import com.yida.result.Result;
import com.yida.security.TokenService;
import com.yida.service.UserService;
import com.yida.vo.UserLoginVO;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/user/user")
public class UserController {
    private final UserService userService;
    private final TokenService tokenService;
    private final JwtProperties jwtProperties;

    public UserController(UserService userService, TokenService tokenService, JwtProperties jwtProperties) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping({"/login", "/wechat-login", "/password-login"})
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO request) {
        User user = userService.login(request);
        return Result.success(UserLoginVO.builder().id(user.getId()).openid(user.getOpenid())
                .token(tokenService.issueUser(user.getId())).build());
    }

    @PostMapping("/logout")
    public Result logout(HttpServletRequest request) {
        tokenService.revokeUser(request.getHeader(jwtProperties.getUserTokenName()));
        return Result.success();
    }
}