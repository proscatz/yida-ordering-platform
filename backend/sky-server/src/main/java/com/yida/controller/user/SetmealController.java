package com.yida.controller.user;

import com.yida.cache.CatalogCacheService;
import com.yida.constant.StatusConstant;
import com.yida.entity.Setmeal;
import com.yida.result.Result;
import com.yida.service.SetmealService;
import com.yida.vo.DishItemVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("userSetmealController")
@RequestMapping("/user/setmeal")
@Api(tags = "C端-套餐浏览接口")
public class SetmealController {
    @Autowired
    private SetmealService setmealService;
    @Autowired
    private CatalogCacheService catalogCacheService;

    @GetMapping("/list")
    @ApiOperation("根据分类id查询套餐")
    public Result<List<Setmeal>> list(Long categoryId) {
        List<Setmeal> list = catalogCacheService.getSetmeals(categoryId, () -> {
            Setmeal setmeal = new Setmeal();
            setmeal.setCategoryId(categoryId);
            setmeal.setStatus(StatusConstant.ENABLE);
            return setmealService.list(setmeal);
        });
        return Result.success(list);
    }

    @GetMapping("/dish/{id}")
    @ApiOperation("根据套餐id查询包含的菜品列表")
    public Result<List<DishItemVO>> dishList(@PathVariable("id") Long id) {
        return Result.success(setmealService.getDishItemById(id));
    }
}