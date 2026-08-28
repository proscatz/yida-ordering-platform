package com.yida.catalog;

import com.yida.cache.CacheKeys;
import com.yida.cache.CatalogCacheService;
import com.yida.YidaApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("dev")
@SpringBootTest(
        classes = YidaApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "spring.rabbitmq.listener.direct.auto-startup=false"
        })
@EnabledIfSystemProperty(named = "catalog.cache.invalidate", matches = "true")
class CatalogCacheInvalidationTest {

    private static final List<Long> DISH_CATEGORY_IDS = Arrays.asList(
            1L, 2L, 3L, 4L, 11L, 12L, 16L, 17L, 18L, 19L, 20L, 21L, 26L, 27L);
    private static final List<Long> SETMEAL_CATEGORY_IDS = Arrays.asList(13L, 15L, 28L);

    @Autowired
    private CatalogCacheService catalogCacheService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void invalidateOnlyCatalogKeysAfterDirectSqlMigration() {
        Object shopStatusBefore = redisTemplate.opsForValue().get(CacheKeys.SHOP_STATUS);
        Object shopStatusSourceBefore = redisTemplate.opsForValue().get(CacheKeys.SHOP_STATUS_SOURCE);

        DISH_CATEGORY_IDS.forEach(catalogCacheService::invalidateDishCategory);
        SETMEAL_CATEGORY_IDS.forEach(catalogCacheService::invalidateSetmealCategory);
        catalogCacheService.invalidateAllDishes();
        catalogCacheService.invalidateAllSetmeals();

        DISH_CATEGORY_IDS.forEach(categoryId ->
                assertThat(redisTemplate.hasKey(CacheKeys.dishCategory(categoryId))).isFalse());
        SETMEAL_CATEGORY_IDS.forEach(categoryId ->
                assertThat(redisTemplate.hasKey(CacheKeys.setmealCategory(categoryId))).isFalse());
        assertThat(redisTemplate.hasKey(CacheKeys.DISH_INDEX)).isFalse();
        assertThat(redisTemplate.hasKey(CacheKeys.SETMEAL_INDEX)).isFalse();
        assertThat(redisTemplate.opsForValue().get(CacheKeys.SHOP_STATUS)).isEqualTo(shopStatusBefore);
        assertThat(redisTemplate.opsForValue().get(CacheKeys.SHOP_STATUS_SOURCE)).isEqualTo(shopStatusSourceBefore);
    }
}
