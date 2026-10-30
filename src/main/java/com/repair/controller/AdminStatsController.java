package com.repair.controller;

import com.repair.common.enums.RoleEnum;
import com.repair.common.result.Result;
import com.repair.dto.DashboardStatsVO;
import com.repair.dto.RepairerLeaderboardVO;
import com.repair.entity.User;
import com.repair.service.DashboardService;
import com.repair.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

/**
 * 管理端数据统计接口（design.md §3.4）。
 */
@Tag(name = "管理端统计模块")
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final DashboardService dashboardService;

    private final UserService userService;

    /** 首页看板统计：指标卡 + 状态分布 + 近 7 天趋势 */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<DashboardStatsVO> dashboard() {
        return Result.success(dashboardService.dashboardStats());
    }

    /**
     * 师傅业绩榜：返回全部 REPAIRER 师傅（含禁用账号），
     * 按累计完成单量降序、评分降序排列，纯展示无操作。
     */
    @GetMapping("/repairers")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<RepairerLeaderboardVO>> repairers() {
        List<RepairerLeaderboardVO> list = userService.listAllByRole(RoleEnum.REPAIRER).stream()
                .map(this::toLeaderboardVO)
                .sorted(Comparator
                        .comparing(RepairerLeaderboardVO::getFinishCount,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(RepairerLeaderboardVO::getRating,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return Result.success(list);
    }

    private RepairerLeaderboardVO toLeaderboardVO(User user) {
        BigDecimal rating = user.getRating() == null
                ? null
                : user.getRating().setScale(1, RoundingMode.HALF_UP);
        return RepairerLeaderboardVO.builder()
                .id(user.getId())
                .name(StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername())
                .username(user.getUsername())
                .finishCount(user.getFinishCount() == null ? 0 : user.getFinishCount())
                .rating(rating)
                .status(user.getStatus())
                .build();
    }
}
