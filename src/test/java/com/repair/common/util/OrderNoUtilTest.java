package com.repair.common.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工单单号工具测试。
 */
class OrderNoUtilTest {

    @Test
    void formatIsROPlusTimestamp() {
        String no = OrderNoUtil.generate();
        assertTrue(no.startsWith("RO"), "单号应以 RO 开头: " + no);
        assertTrue(no.matches("RO\\d{20}"), "单号应为 RO + 14 位时间戳 + 6 位序列: " + no);
    }

    @Test
    void uniquenessWithinBatch() {
        Set<String> set = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            assertTrue(set.add(OrderNoUtil.generate()), "单号不应重复");
        }
    }
}
