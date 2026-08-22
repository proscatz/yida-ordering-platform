package com.yida.mapper;

import com.yida.entity.AddressBook;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AddressBookMapper {

    List<AddressBook> list(AddressBook addressBook);

    @Insert("insert into address_book" +
            " (user_id, consignee, phone, sex, province_code, province_name, city_code, city_name, district_code," +
            " district_name, detail, label, is_default)" +
            " values (#{userId}, #{consignee}, #{phone}, #{sex}, #{provinceCode}, #{provinceName}, #{cityCode}, #{cityName}," +
            " #{districtCode}, #{districtName}, #{detail}, #{label}, #{isDefault})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AddressBook addressBook);

    @Select("select * from address_book where id = #{id}")
    AddressBook getById(Long id);

    @Select("select * from address_book where id = #{id} and user_id = #{userId}")
    AddressBook getByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    int update(AddressBook addressBook);

    @Update("update address_book set is_default = 0 where user_id = #{userId} and is_default = 1")
    int clearDefaultByUserId(@Param("userId") Long userId);

    @Update("update address_book set is_default = 1 where id = #{id} and user_id = #{userId}")
    int setDefaultByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Delete("delete from address_book where id = #{id} and user_id = #{userId}")
    int deleteById(@Param("id") Long id, @Param("userId") Long userId);

    @Select("select id from user where id = #{userId} for update")
    Long lockUserRow(@Param("userId") Long userId);

    @Select("select * from address_book where user_id = #{userId} and is_default = 1 order by id desc limit 1")
    AddressBook getDefaultByUserId(@Param("userId") Long userId);
}
