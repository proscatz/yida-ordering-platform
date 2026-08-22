package com.yida.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Getter
@Setter
public abstract class AddressBookBaseDTO {
    private static final String NO_CONTROL_CHARACTERS = "^[^\\p{Cc}\\p{Cf}]*$";

    @NotBlank(message = "请输入联系人")
    @Size(min = 2, max = 30, message = "联系人长度应为2～30个字符")
    @Pattern(regexp = NO_CONTROL_CHARACTERS, message = "联系人不能包含控制字符")
    private String consignee;

    @NotBlank(message = "请输入手机号")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的手机号")
    private String phone;

    @NotBlank(message = "请选择称呼")
    @Pattern(regexp = "^[01]$", message = "称呼参数不正确")
    private String sex;

    @NotBlank(message = "请选择省份")
    @Pattern(regexp = "^[1-9]\\d{5}$", message = "省级区划代码格式不正确")
    private String provinceCode;

    @NotBlank(message = "请选择省份")
    @Size(max = 30, message = "省份名称过长")
    @Pattern(regexp = NO_CONTROL_CHARACTERS, message = "省份名称不能包含控制字符")
    private String provinceName;

    @NotBlank(message = "请选择城市")
    @Pattern(regexp = "^[1-9]\\d{5}$", message = "市级区划代码格式不正确")
    private String cityCode;

    @NotBlank(message = "请选择城市")
    @Size(max = 30, message = "城市名称过长")
    @Pattern(regexp = NO_CONTROL_CHARACTERS, message = "城市名称不能包含控制字符")
    private String cityName;

    @NotBlank(message = "请选择区县")
    @Pattern(regexp = "^[1-9]\\d{5}$", message = "区级区划代码格式不正确")
    private String districtCode;

    @NotBlank(message = "请选择区县")
    @Size(max = 30, message = "区县名称过长")
    @Pattern(regexp = NO_CONTROL_CHARACTERS, message = "区县名称不能包含控制字符")
    private String districtName;

    @NotBlank(message = "请输入详细地址")
    @Size(min = 5, max = 200, message = "详细地址长度应为5～200个字符")
    @Pattern(regexp = NO_CONTROL_CHARACTERS, message = "详细地址不能包含控制字符")
    private String detail;

    @Size(max = 20, message = "标签最多20个字符")
    @Pattern(regexp = NO_CONTROL_CHARACTERS, message = "标签不能包含控制字符")
    private String label;

    @NotNull(message = "默认地址参数不能为空")
    @javax.validation.constraints.Min(value = 0, message = "默认地址参数不正确")
    @javax.validation.constraints.Max(value = 1, message = "默认地址参数不正确")
    private Integer isDefault;

    public void setConsignee(String value) { this.consignee = trim(value); }
    public void setPhone(String value) { this.phone = trim(value); }
    public void setSex(String value) { this.sex = trim(value); }
    public void setProvinceCode(String value) { this.provinceCode = trim(value); }
    public void setProvinceName(String value) { this.provinceName = trim(value); }
    public void setCityCode(String value) { this.cityCode = trim(value); }
    public void setCityName(String value) { this.cityName = trim(value); }
    public void setDistrictCode(String value) { this.districtCode = trim(value); }
    public void setDistrictName(String value) { this.districtName = trim(value); }
    public void setDetail(String value) { this.detail = trim(value); }
    public void setLabel(String value) { this.label = trimToNull(value); }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }
}
