package com.repair.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端首页统计视图（design.md §3.4 GET /api/admin/stats/dashboard）。
 * 前端数据看板一次拉取：指标卡 / 状态分布环形图 / 近 7 天趋势折线图。
 */
@Data
@Builder
public class DashboardStatsVO {

    /** 总工单数（deleted=0 的全部工单） */
    private Long totalOrders;

    /** 待处理工单数（PENDING） */
    private Long pendingCount;

    /** 维修中工单数（PROCESSING） */
    private Long processingCount;

    /** 完成率（%）：(COMPLETED + CONFIRMED) / totalOrders，保留 1 位小数 */
    private BigDecimal completionRate;

    /** 状态分布（固定 7 态，用于环形图） */
    private List<StatusItem> statusDistribution;

    /** 近 7 天新增/完成趋势（固定 7 项，用于折线图） */
    private List<TrendItem> trend;

    /**
     * 状态分布条目：数量为 0 的状态也返回，保证环形图图例稳定。
     */
    @Data
    @Builder
    public static class StatusItem {

        /** 状态码，见 OrderStatus */
        private String status;

        /** 状态中文名 */
        private String name;

        /** 该状态工单数 */
        private Long count;

        /** 占总数百分比（%），保留 1 位小数 */
        private BigDecimal percentage;
    }

    /**
     * 趋势条目：单日新增数与完成数。
     */
    @Data
    @Builder
    public static class TrendItem {

        /** 日期（MM-dd 展示格式） */
        private String date;

        /** 当日新增工单数（按 created_at 统计） */
        private Long newCount;

        /** 当日完成工单数（按 finish_time 统计） */
        private Long doneCount;
    }
}
