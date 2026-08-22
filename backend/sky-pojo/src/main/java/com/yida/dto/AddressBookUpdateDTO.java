package com.yida.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Getter
@Setter
public class AddressBookUpdateDTO extends AddressBookBaseDTO {
    @NotNull(message = "地址ID不能为空")
    @Positive(message = "地址ID不正确")
    private Long id;
}
