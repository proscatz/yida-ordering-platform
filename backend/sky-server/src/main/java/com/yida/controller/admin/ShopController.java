package com.yida.controller.admin;

import com.yida.cache.CatalogCacheService;
import com.yida.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("adminShopController")
@RequestMapping("/admin/shop")
public class ShopController {
    @Autowired
    private CatalogCacheService catalogCacheService;

    @PutMapping("/{status}")
    public Result setStatus(@PathVariable Integer status) {
        catalogCacheService.setShopStatus(status);
        return Result.success();
    }

    @GetMapping("/status")
    public Result<Integer> getStatus() {
        return Result.success(catalogCacheService.getShopStatus());
    }
}