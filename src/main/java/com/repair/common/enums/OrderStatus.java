package com.repair.common.enums;

import com.repair.common.exception.BusinessException;
import com.repair.common.result.ErrorCode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 工单状态机（design.md §1）。
 * 状态用 varchar 常量存储，不使用魔法数字；
 * ALLOWED_TRANSITIONS 严格对应 §1.3「各状态允许的操作」表，非法跳转统一抛业务异常。
 */
public final class OrderStatus {

    /** 待受理 */
    public static final String PENDING = "PENDING";

    /** 已派单 */
    public static final String ASSIGNED = "ASSIGNED";

    /** 维修中 */
    public static final String PROCESSING = "PROCESSING";

    /** 已完成（待报修人确认） */
    public static final String COMPLETED = "COMPLETED";

    /** 已确认（终态） */
    public static final String CONFIRMED = "CONFIRMED";

    /** 已取消（终态） */
    public static final String CANCELLED = "CANCELLED";

    /** 已驳回（终态） */
    public static final String REJECTED = "REJECTED";

    /** 申请改派中（师傅有事/不想修，等管理员审批） */
    public static final String REASSIGNING = "REASSIGNING";

    /** 恶意报修仲裁中（师傅申请驳回报修，等管理员审批） */
    public static final String ARBITRATING = "ARBITRATING";

    /**
     * 状态跳转表（design.md §1.3 + 改派/仲裁审批闭环）：
     * PENDING    -> ASSIGNED(派单) / REJECTED(驳回) / CANCELLED(取消)
     * ASSIGNED   -> PROCESSING(接单) / PENDING(拒单) / ASSIGNED(改派) / CANCELLED(管理员取消) / REASSIGNING(申请改派)
     * PROCESSING -> COMPLETED(完成) / ASSIGNED(改派) / ARBITRATING(申请仲裁)
     * REASSIGNING-> PENDING(同意改派，重新派单) / ASSIGNED(驳回改派，继续接单)
     * ARBITRATING-> REJECTED(同意仲裁，驳回工单) / PROCESSING(驳回仲裁，打回重修)
     * COMPLETED  -> CONFIRMED(确认评价)
     * 终态无出口。
     */
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            PENDING, Set.of(ASSIGNED, REJECTED, CANCELLED),
            ASSIGNED, Set.of(PROCESSING, PENDING, ASSIGNED, CANCELLED, REASSIGNING),
            PROCESSING, Set.of(COMPLETED, ASSIGNED, ARBITRATING),
            REASSIGNING, Set.of(PENDING, ASSIGNED),
            ARBITRATING, Set.of(REJECTED, PROCESSING),
            COMPLETED, Set.of(CONFIRMED),
            CONFIRMED, Set.of(),
            CANCELLED, Set.of(),
            REJECTED, Set.of()
    );

    /** 状态码 → 中文名（与前端 utils/status.ts 的 ORDER_STATUS_MAP 完全一致） */
    private static final Map<String, String> STATUS_LABELS = Map.of(
            PENDING, "待受理",
            ASSIGNED, "已派单",
            PROCESSING, "维修中",
            COMPLETED, "已完成",
            CONFIRMED, "已确认",
            CANCELLED, "已取消",
            REJECTED, "已驳回",
            REASSIGNING, "申请改派中",
            ARBITRATING, "仲裁中"
    );

    private OrderStatus() {
    }

    /**
     * 状态码转中文名，未知状态码回退返回原码（导出/展示兜底，不抛异常）。
     */
    public static String toLabel(String status) {
        return STATUS_LABELS.getOrDefault(status, status);
    }

    /**
     * 校验 from -> to 是否合法，非法抛 ORDER_STATUS_INVALID（400）。
     */
    public static void requireTransition(String from, String to) {
        Set<String> allowed = ALLOWED_TRANSITIONS.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
    }

    /** 是否终态：终态后不允许任何操作 */
    public static boolean isTerminal(String status) {
        return CONFIRMED.equals(status) || CANCELLED.equals(status) || REJECTED.equals(status);
    }

    /** 是否为合法状态码 */
    public static boolean isValid(String status) {
        return ALLOWED_TRANSITIONS.containsKey(status);
    }

    /**
     * 解析逗号分隔的状态列表，供「按多个状态 in 查询」使用。
     * 前端 Tab 切换会传形如 {@code PENDING,ASSIGNED,PROCESSING} 的字符串。
     * null / 空白返回 null（调用方应据此跳过状态过滤条件）；
     * 其余按逗号拆分、trim、过滤空串并去重。
     * 不做状态码合法性校验：非法码只会自然查不到数据，无安全风险。
     */
    public static List<String> parseCsv(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return Arrays.stream(status.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }
}
