package com.repair.controller;

import com.repair.common.result.Result;
import com.repair.dto.DashboardStatsVO;
import com.repair.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端数据统计接口（design.md §3.4）。
 */
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final DashboardService dashboardService;

    /** 首页看板统计：指标卡 + 状态分布 + 近 7 天趋势 */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<DashboardStatsVO> dashboard() {
        return Result.success(dashboardService.dashboardStats());
    }
}
