package com.yida.controller.admin;

import com.yida.cache.CatalogCacheService;
import com.yida.dto.SetmealDTO;
import com.yida.service.SetmealService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SetmealControllerCacheTest {
    @Mock
    private SetmealService setmealService;
    @Mock
    private CatalogCacheService catalogCacheService;
    @InjectMocks
    private SetmealController setmealController;

    @Test
    void saveEvictsOnlyTheAffectedSetmealCategoryCache() {
        SetmealDTO request = new SetmealDTO();
        request.setCategoryId(10L);
        setmealController.save(request);
        verify(setmealService).saveWithDish(request);
        verify(catalogCacheService).invalidateSetmealCategory(10L);
    }

    @Test
    void updateEvictsAllMaintainedSetmealCaches() {
        SetmealDTO request = new SetmealDTO();
        request.setId(1L);
        setmealController.update(request);
        verify(setmealService).update(request);
        verify(catalogCacheService).invalidateAllSetmeals();
    }
}