package com.repair.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 维修师傅统计视图（design.md §3.3 GET /api/repairer/stats）。
 */
@Data
@Builder
public class RepairerStatsVO {

    /** 累计完成单量 */
    private Integer finishCount;

    /** 平均评分（暂无评价时为 null） */
    private BigDecimal rating;

    /** 当前维修中（PROCESSING）单量 */
    private Long processingCount;

    /** 待接单（ASSIGNED）单量 */
    private Long assignedCount;
}
