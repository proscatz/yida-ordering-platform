package com.yida.cache;

import com.yida.entity.Setmeal;
import com.yida.vo.DishVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.UUID;

@Service
public class CatalogCacheService {
    static final String NULL_VALUE = "__YIDA_CACHE_NULL__";
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    private static final int LOCK_RETRY_COUNT = 40;
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end",
            Long.class);
    private static final long LOCK_RETRY_MILLIS = 25L;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private CacheTtlPolicy ttlPolicy;

    public List<DishVO> getDishes(Long categoryId, Supplier<List<DishVO>> loader) {
        return getList(CacheKeys.dishCategory(categoryId), CacheKeys.DISH_INDEX, loader);
    }

    public List<Setmeal> getSetmeals(Long categoryId, Supplier<List<Setmeal>> loader) {
        return getList(CacheKeys.setmealCategory(categoryId), CacheKeys.SETMEAL_INDEX, loader);
    }

    public Integer getShopStatus() {
        Object cached = redisTemplate.opsForValue().get(CacheKeys.SHOP_STATUS);
        if (cached instanceof Integer) {
            return (Integer) cached;
        }
        return rebuild(CacheKeys.SHOP_STATUS, null, () -> {
            Object source = redisTemplate.opsForValue().get(CacheKeys.SHOP_STATUS_SOURCE);
            return source instanceof Integer ? (Integer) source : 0;
        }, false, ttlPolicy.shopStatus());
    }

    public void setShopStatus(Integer status) {
        redisTemplate.opsForValue().set(CacheKeys.SHOP_STATUS_SOURCE, status);
        redisTemplate.opsForValue().set(CacheKeys.SHOP_STATUS, status, ttlPolicy.shopStatus());
    }

    public void invalidateDishCategory(Long categoryId) {
        invalidateExact(CacheKeys.DISH_INDEX, CacheKeys.dishCategory(categoryId));
    }

    public void invalidateAllDishes() {
        invalidateIndex(CacheKeys.DISH_INDEX);
    }

    public void invalidateSetmealCategory(Long categoryId) {
        invalidateExact(CacheKeys.SETMEAL_INDEX, CacheKeys.setmealCategory(categoryId));
    }

    public void invalidateAllSetmeals() {
        invalidateIndex(CacheKeys.SETMEAL_INDEX);
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getList(String key, String indexKey, Supplier<List<T>> loader) {
        Object cached = redisTemplate.opsForValue().get(key);
        if (NULL_VALUE.equals(cached)) {
            return Collections.emptyList();
        }
        if (cached instanceof List) {
            return (List<T>) cached;
        }
        return rebuild(key, indexKey, loader, true, null);
    }

    @SuppressWarnings("unchecked")
    private <T> T rebuild(String key, String indexKey, Supplier<T> loader,
                          boolean emptyAware, Duration fixedTtl) {
        String lockKey = CacheKeys.rebuildLock(key);
        for (int attempt = 0; attempt <= LOCK_RETRY_COUNT; attempt++) {
            String lockToken = UUID.randomUUID().toString();
            Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, lockToken, LOCK_TTL);
            if (Boolean.TRUE.equals(locked)) {
                try {
                    Object doubleChecked = redisTemplate.opsForValue().get(key);
                    if (NULL_VALUE.equals(doubleChecked)) return (T) Collections.emptyList();
                    if (doubleChecked != null) return (T) doubleChecked;
                    T loaded = loader.get();
                    boolean empty = loaded == null || (emptyAware && loaded instanceof Collection
                            && ((Collection<?>) loaded).isEmpty());
                    Object value = empty ? NULL_VALUE : loaded;
                    Duration ttl = fixedTtl != null ? fixedTtl : (empty ? ttlPolicy.empty() : ttlPolicy.normal());
                    redisTemplate.opsForValue().set(key, value, ttl);
                    if (indexKey != null) redisTemplate.opsForSet().add(indexKey, key);
                    return empty ? (T) Collections.emptyList() : loaded;
                } finally {
                    redisTemplate.execute(RELEASE_LOCK_SCRIPT, Collections.singletonList(lockKey), lockToken);
                }
            }
            Object refreshed = redisTemplate.opsForValue().get(key);
            if (NULL_VALUE.equals(refreshed)) return (T) Collections.emptyList();
            if (refreshed != null) return (T) refreshed;
            pauseBeforeRetry();
        }
        throw new IllegalStateException("缓存重建繁忙，请稍后重试");
    }

    private void invalidateExact(String indexKey, String key) {
        redisTemplate.delete(key);
        redisTemplate.opsForSet().remove(indexKey, key);
    }

    private void invalidateIndex(String indexKey) {
        Set<Object> keys = redisTemplate.opsForSet().members(indexKey);
        if (keys != null) {
            for (Object key : keys) {
                if (key instanceof String) redisTemplate.delete((String) key);
            }
        }
        redisTemplate.delete(indexKey);
    }

    private void pauseBeforeRetry() {
        try {
            Thread.sleep(LOCK_RETRY_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待缓存重建时线程被中断", exception);
        }
    }
}