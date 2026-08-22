package com.yida.controller.user;

import com.yida.cache.CatalogCacheService;
import com.yida.constant.StatusConstant;
import com.yida.entity.Dish;
import com.yida.result.Result;
import com.yida.service.DishService;
import com.yida.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private CatalogCacheService catalogCacheService;

    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        List<DishVO> list = catalogCacheService.getDishes(categoryId, () -> {
            Dish dish = new Dish();
            dish.setCategoryId(categoryId);
            dish.setStatus(StatusConstant.ENABLE);
            return dishService.listWithFlavor(dish);
        });
        return Result.success(list);
    }
}