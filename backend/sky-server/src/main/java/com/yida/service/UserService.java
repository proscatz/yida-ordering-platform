package com.yida.service;

import com.yida.dto.UserLoginDTO;
import com.yida.entity.User;

public interface UserService {

    User login(UserLoginDTO userLoginDTO);
}
