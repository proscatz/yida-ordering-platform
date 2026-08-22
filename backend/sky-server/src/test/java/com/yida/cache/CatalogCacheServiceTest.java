package com.yida.cache;

import com.yida.vo.DishVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogCacheServiceTest {
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> values;
    @Mock
    private SetOperations<String, Object> sets;
    private CatalogCacheService service;

    @BeforeEach
    void setUp() {
        service = new CatalogCacheService();
        ReflectionTestUtils.setField(service, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(service, "ttlPolicy", new CacheTtlPolicy());
        lenient().when(redisTemplate.opsForValue()).thenReturn(values);
    }

    @Test
    void emptyResultUsesShortNullCacheAndPreventsRepeatedLoad() {
        String key = CacheKeys.dishCategory(10L);
        when(values.get(key)).thenReturn(null, null, CatalogCacheService.NULL_VALUE);
        when(values.setIfAbsent(eq(CacheKeys.rebuildLock(key)), anyString(), any(Duration.class))).thenReturn(true);
        when(redisTemplate.opsForSet()).thenReturn(sets);
        AtomicInteger loads = new AtomicInteger();

        List<DishVO> first = service.getDishes(10L, () -> { loads.incrementAndGet(); return Collections.emptyList(); });
        List<DishVO> second = service.getDishes(10L, () -> { loads.incrementAndGet(); return Collections.emptyList(); });

        assertTrue(first.isEmpty());
        assertTrue(second.isEmpty());
        assertEquals(1, loads.get());
        ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        verify(values).set(eq(key), eq(CatalogCacheService.NULL_VALUE), ttl.capture());
        assertTrue(ttl.getValue().getSeconds() >= 60 && ttl.getValue().getSeconds() <= 90);
    }

    @Test
    void onlyLockOwnerRebuildsHotCache() {
        String key = CacheKeys.dishCategory(11L);
        DishVO cached = new DishVO();
        when(values.get(key)).thenReturn(null, null, List.of(cached));
        when(values.setIfAbsent(eq(CacheKeys.rebuildLock(key)), anyString(), any(Duration.class))).thenReturn(false);
        AtomicInteger loads = new AtomicInteger();

        List<DishVO> result = service.getDishes(11L, () -> { loads.incrementAndGet(); return Collections.emptyList(); });

        assertEquals(1, result.size());
        assertEquals(0, loads.get());
    }

    @Test
    void invalidationUsesMaintainedKeySetAndNeverRedisKeysScan() {
        when(redisTemplate.opsForSet()).thenReturn(sets);
        when(sets.members(CacheKeys.DISH_INDEX)).thenReturn(Set.of(
                CacheKeys.dishCategory(10L), CacheKeys.dishCategory(11L)));

        service.invalidateAllDishes();

        verify(redisTemplate).delete(CacheKeys.dishCategory(10L));
        verify(redisTemplate).delete(CacheKeys.dishCategory(11L));
        verify(redisTemplate).delete(CacheKeys.DISH_INDEX);
        verify(redisTemplate, never()).keys(anyString());
    }
}