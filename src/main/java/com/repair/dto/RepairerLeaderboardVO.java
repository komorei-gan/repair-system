package com.repair.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 师傅业绩榜视图（GET /api/admin/stats/repairers）。
 * 纯展示：累计完成单量、平均评分（保留 1 位小数）、账号状态。
 */
@Data
@Builder
public class RepairerLeaderboardVO {

    private Long id;

    /** 姓名（优先 realName，回退 username） */
    private String name;

    /** 账号 */
    private String username;

    /** 累计完成单量 */
    private Integer finishCount;

    /** 平均评分（暂无评价时为 null） */
    private BigDecimal rating;

    /** 1 启用 / 0 禁用 */
    private Integer status;
}
