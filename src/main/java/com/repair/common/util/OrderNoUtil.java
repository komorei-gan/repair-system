package com.repair.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 工单业务单号工具：RO + yyyyMMddHHmmss + 6 位进程内单调序列（随机起点）。
 * 单机部署下进程内保证唯一（序列单调递增，百万级/秒足够）；
 * 跨重启由时间戳 + 随机起点兜底，数据库唯一索引仍是最后防线。
 */
public final class OrderNoUtil {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final long SEQUENCE_MOD = 1_000_000L;

    /** 随机起点，避免重启后与上次运行序列撞车 */
    private static final AtomicLong SEQUENCE =
            new AtomicLong(ThreadLocalRandom.current().nextLong(SEQUENCE_MOD));

    private OrderNoUtil() {
    }

    public static String generate() {
        long seq = SEQUENCE.incrementAndGet() % SEQUENCE_MOD;
        return "RO" + LocalDateTime.now().format(FORMATTER) + String.format("%06d", seq);
    }
}
