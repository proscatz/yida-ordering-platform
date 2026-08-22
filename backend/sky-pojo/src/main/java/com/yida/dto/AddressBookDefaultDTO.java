package com.yida.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Data
public class AddressBookDefaultDTO {
    @NotNull(message = "地址ID不能为空")
    @Positive(message = "地址ID不正确")
    private Long id;
}
