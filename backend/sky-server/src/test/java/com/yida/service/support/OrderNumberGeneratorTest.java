package com.yida.service.support;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderNumberGeneratorTest {

    @Test
    void generatesFixedLengthHexadecimalOrderNumbersWithoutDuplicatesInSample() {
        OrderNumberGenerator generator = new OrderNumberGenerator();
        Set<String> generated = new HashSet<>();

        for (int index = 0; index < 10_000; index++) {
            String orderNumber = generator.next();
            assertTrue(orderNumber.matches("[0-9a-f]{32}"));
            assertTrue(generated.add(orderNumber));
        }

        assertEquals(10_000, generated.size());
    }
}