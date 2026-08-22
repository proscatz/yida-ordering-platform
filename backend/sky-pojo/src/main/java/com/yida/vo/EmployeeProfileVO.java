package com.yida.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeProfileVO implements Serializable {
    private Long id;
    private String username;
    private String name;
    private String phone;
    private String sex;
    private Integer status;
    private String role;
}
