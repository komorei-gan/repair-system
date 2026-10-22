package com.repair.service;

import com.repair.dto.DashboardStatsVO;

/**
 * 管理端数据看板统计（design.md §3.4 GET /api/admin/stats/dashboard）。
 */
public interface DashboardService {

    /** 首页统计：指标卡 + 状态分布 + 近 7 天趋势 */
    DashboardStatsVO dashboardStats();
}
