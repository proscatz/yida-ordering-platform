package com.yida.mapper;

import com.github.pagehelper.Page;
import com.yida.annotation.Autofill;
import com.yida.dto.SetmealPageQueryDTO;
import com.yida.entity.Setmeal;
import com.yida.enumeration.OperationType;
import com.yida.vo.DishItemVO;
import com.yida.vo.SetmealVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SetmealMapper {

    /**
     * 根据分类id查询套餐的数量
     * @param id
     * @return
     */
    @Select("select count(id) from setmeal where category_id = #{categoryId}")
    Integer countByCategoryId(Long id);

    List<Setmeal> list(Setmeal setmeal);

    @Select("select sd.name, sd.copies, d.image, d.description " +
            "from setmeal_dish sd left join dish d on sd.dish_id = d.id " +
            "where sd.setmeal_id = #{setmealId}")
    List<DishItemVO> getDishItemBySetmealId(Long setmealId);

    void update(Setmeal setmeal);

    @Autofill(OperationType.INSERT)
    void insert(Setmeal setmeal);

    Page<SetmealVO> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    @Select("select * from setmeal where id = #{id}")
    Setmeal getById(Long id);

    @Select("select * from setmeal where id = #{id} for update")
    Setmeal getByIdForUpdate(Long id);

    @Delete("delete from setmeal where id = #{id}")
    void deleteById(Long setmealId);

    Integer countByMap(Map map);
}
