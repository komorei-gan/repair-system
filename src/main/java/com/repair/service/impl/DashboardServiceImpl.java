package com.repair.service.impl;

import com.repair.common.enums.OrderStatus;
import com.repair.dto.DashboardStatsVO;
import com.repair.mapper.RepairOrderMapper;
import com.repair.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端数据看板统计实现。
 * 两次聚合查询出全部数据：
 * 1) selectStatusCount —— 各状态数量，派生总单数 / 待处理 / 维修中 / 完成率 / 状态分布；
 * 2) selectTrend —— 近 7 天每日新增与完成数。
 * 空库与除零均已保护。
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final RepairOrderMapper repairOrderMapper;

    /** 状态码 → 中文名（与前端 utils/status.ts 一致） */
    private static final Map<String, String> STATUS_NAMES = Map.of(
            OrderStatus.PENDING, "待受理",
            OrderStatus.ASSIGNED, "已派单",
            OrderStatus.PROCESSING, "维修中",
            OrderStatus.COMPLETED, "已完成",
            OrderStatus.CONFIRMED, "已确认",
            OrderStatus.CANCELLED, "已取消",
            OrderStatus.REJECTED, "已驳回");

    /** 状态分布展示顺序（与状态机流转顺序一致） */
    private static final List<String> STATUS_ORDER = List.of(
            OrderStatus.PENDING,
            OrderStatus.ASSIGNED,
            OrderStatus.PROCESSING,
            OrderStatus.COMPLETED,
            OrderStatus.CONFIRMED,
            OrderStatus.CANCELLED,
            OrderStatus.REJECTED);

    private static final DateTimeFormatter DAY_KEY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MM-dd");

    @Override
    public DashboardStatsVO dashboardStats() {
        // 各状态数量：status -> cnt
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (Map<String, Object> row : repairOrderMapper.selectStatusCount()) {
            String status = String.valueOf(row.get("status"));
            statusCounts.put(status, toLong(row.get("cnt")));
        }

        long totalOrders = statusCounts.values().stream().mapToLong(Long::longValue).sum();
        long pendingCount = statusCounts.getOrDefault(OrderStatus.PENDING, 0L);
        long processingCount = statusCounts.getOrDefault(OrderStatus.PROCESSING, 0L);
        long completedConfirmed = statusCounts.getOrDefault(OrderStatus.COMPLETED, 0L)
                + statusCounts.getOrDefault(OrderStatus.CONFIRMED, 0L);
        BigDecimal completionRate = percent(completedConfirmed, totalOrders);

        // 状态分布：固定 7 态，缺失补 0
        List<DashboardStatsVO.StatusItem> distribution = STATUS_ORDER.stream()
                .map(status -> DashboardStatsVO.StatusItem.builder()
                        .status(status)
                        .name(STATUS_NAMES.get(status))
                        .count(statusCounts.getOrDefault(status, 0L))
                        .percentage(percent(statusCounts.getOrDefault(status, 0L), totalOrders))
                        .build())
                .toList();

        // 近 7 天趋势：固定 7 天，缺失补 0
        List<DashboardStatsVO.TrendItem> trend = buildTrend();

        return DashboardStatsVO.builder()
                .totalOrders(totalOrders)
                .pendingCount(pendingCount)
                .processingCount(processingCount)
                .completionRate(completionRate)
                .statusDistribution(distribution)
                .trend(trend)
                .build();
    }

    /** 近 7 天（含今天）每日新增/完成数，无数据的日期补 0 */
    private List<DashboardStatsVO.TrendItem> buildTrend() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusDays(6).atStartOfDay();

        Map<String, long[]> dayCounts = new LinkedHashMap<>();
        for (Map<String, Object> row : repairOrderMapper.selectTrend(start)) {
            String day = String.valueOf(row.get("day"));
            dayCounts.put(day, new long[]{toLong(row.get("new_count")), toLong(row.get("done_count"))});
        }

        return java.util.stream.IntStream.range(0, 7)
                .mapToObj(today::minusDays)
                .sorted()
                .map(day -> {
                    long[] counts = dayCounts.getOrDefault(day.format(DAY_KEY), new long[]{0L, 0L});
                    return DashboardStatsVO.TrendItem.builder()
                            .date(day.format(DAY_LABEL))
                            .newCount(counts[0])
                            .doneCount(counts[1])
                            .build();
                })
                .toList();
    }

    /** 百分比：分子/分母 × 100，保留 1 位小数；分母为 0 时返回 0 */
    private BigDecimal percent(long numerator, long denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }

    /** 归一聚合结果数值（COUNT/SUM 可能是 Long 或 BigDecimal） */
    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
