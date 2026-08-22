package com.yida.dto;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@ToString(exclude = "password")
public class UserLoginDTO implements Serializable {
    private String code;
    private String username;
    private String phone;
    private String password;
}