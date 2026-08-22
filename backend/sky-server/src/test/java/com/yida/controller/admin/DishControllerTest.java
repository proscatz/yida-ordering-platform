package com.yida.controller.admin;

import com.yida.cache.CatalogCacheService;
import com.yida.dto.DishDTO;
import com.yida.service.DishService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DishControllerTest {
    @Mock
    private DishService dishService;
    @Mock
    private CatalogCacheService catalogCacheService;
    @InjectMocks
    private DishController dishController;

    @Test
    void saveEvictsOnlyTheAffectedDishCategoryCache() {
        DishDTO request = new DishDTO();
        request.setCategoryId(10L);
        dishController.save(request);
        verify(dishService).saveWithFlavor(request);
        verify(catalogCacheService).invalidateDishCategory(10L);
    }

    @Test
    void updateEvictsAllMaintainedDishListCaches() {
        DishDTO request = new DishDTO();
        request.setId(1L);
        dishController.update(request);
        verify(dishService).updateWithFlavor(request);
        verify(catalogCacheService).invalidateAllDishes();
    }
}