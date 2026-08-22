package com.yida.service;

import com.yida.dto.DishDTO;
import com.yida.dto.DishPageQueryDTO;
import com.yida.entity.Dish;
import com.yida.result.PageResult;
import com.yida.vo.DishVO;

import java.util.List;


public interface DishService {
    public void saveWithFlavor(DishDTO dishDTO);

    PageResult page(DishPageQueryDTO dishPageQueryDTO);

    void delete(List<Long> ids);

    DishVO getByIdWithFlavor(Long id);

    void updateWithFlavor(DishDTO dishDTO);

    List<DishVO> listWithFlavor(Dish dish);

    void startOrStop(Integer status, Long id);
}
