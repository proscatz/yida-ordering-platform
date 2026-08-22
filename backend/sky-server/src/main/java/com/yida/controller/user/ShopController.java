package com.yida.controller.user;

import com.yida.cache.CatalogCacheService;
import com.yida.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("userShopController")
@RequestMapping("/user/shop")
public class ShopController {
    @Autowired
    private CatalogCacheService catalogCacheService;

    @GetMapping("/status")
    public Result<Integer> getStatus() {
        return Result.success(catalogCacheService.getShopStatus());
    }
}