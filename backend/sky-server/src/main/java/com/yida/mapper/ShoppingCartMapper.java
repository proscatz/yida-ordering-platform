package com.yida.mapper;

import com.yida.dto.ShoppingCartDTO;
import com.yida.entity.ShoppingCart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {
    List<ShoppingCart>list(ShoppingCart shoppingCart);

    @Select("select * from shopping_cart where user_id = #{userId} order by id for update")
    List<ShoppingCart> listByUserIdForUpdate(Long userId);
    @Update("update shopping_cart set number = #{number} where id = #{id}")
    void updateNumberById(ShoppingCart shoppingCart);
    @Insert("insert into shopping_cart (name, user_id, dish_id, setmeal_id, dish_flavor, number, amount, image, create_time) " +
            "values (#{name}, #{userId}, #{dishId}, #{setmealId}, #{dishFlavor}, #{number}, #{amount},#{image}, #{createTime})")
    void insert(ShoppingCart shoppingCart);
    @Delete("delete from shopping_cart where user_id = #{userId}")
    void deleteByUserId(Long userId);
    @Delete("delete from shopping_cart where id = #{Id}")
    void deleteById(Long Id);

    void insertBatch(List<ShoppingCart> shoppingCartList);
}
