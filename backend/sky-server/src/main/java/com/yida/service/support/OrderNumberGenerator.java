package com.yida.service.support;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 生成固定长度的高熵订单号，最终唯一性由数据库唯一约束保证。
 */
@Component
public class OrderNumberGenerator {

    public String next() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}