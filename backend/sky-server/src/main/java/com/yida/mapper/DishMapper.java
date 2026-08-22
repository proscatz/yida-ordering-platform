package com.yida.mapper;

import com.github.pagehelper.Page;
import com.yida.annotation.Autofill;
import com.yida.dto.DishPageQueryDTO;
import com.yida.entity.Dish;
import com.yida.enumeration.OperationType;
import com.yida.vo.DishVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DishMapper {
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);
    @Autofill(value = OperationType.INSERT)
    void insert(Dish dish);

    Page<DishVO> page(DishPageQueryDTO dishPageQueryDTO);

    @Select("select * from dish where id = #{id}")
    Dish getById(Long id);

    @Select("select * from dish where id = #{id} for update")
    Dish getByIdForUpdate(Long id);
    @Delete("delete from dish where id = #{id}")
    void deleteById(Long id);
    @Autofill(value = OperationType.UPDATE)
    void update(Dish dish);

    List<Dish> list(Dish dish);

    @Select("select a.* from dish a left join setmeal_dish b on a.id = b.dish_id where b.setmeal_id = #{setmealId}")
    List<Dish> getBySetmealId(Long setmealId);

    Integer countByMap(Map map);
}
