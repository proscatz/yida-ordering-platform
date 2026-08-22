package com.yida.cache;

public final class CacheKeys {
    private static final String PREFIX = "yida:v1:";
    public static final String DISH_INDEX = PREFIX + "cache:index:dish";
    public static final String SETMEAL_INDEX = PREFIX + "cache:index:setmeal";
    public static final String SHOP_STATUS = PREFIX + "shop:status";
    public static final String SHOP_STATUS_SOURCE = PREFIX + "state:shop:status";

    private CacheKeys() {
    }

    public static String dishCategory(Long categoryId) {
        return PREFIX + "dish:category:" + categoryId;
    }

    public static String setmealCategory(Long categoryId) {
        return PREFIX + "setmeal:category:" + categoryId;
    }

    public static String rebuildLock(String cacheKey) {
        return PREFIX + "lock:cache:" + cacheKey.substring(PREFIX.length());
    }
}