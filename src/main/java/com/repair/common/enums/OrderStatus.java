package com.repair.common.enums;

import com.repair.common.exception.BusinessException;
import com.repair.common.result.ErrorCode;

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

    /**
     * 状态跳转表（design.md §1.3 逐条编码）：
     * PENDING   -> ASSIGNED(派单) / REJECTED(驳回) / CANCELLED(取消)
     * ASSIGNED  -> PROCESSING(接单) / PENDING(拒单) / ASSIGNED(改派) / CANCELLED(管理员取消)
     * PROCESSING-> COMPLETED(完成) / ASSIGNED(改派)
     * COMPLETED -> CONFIRMED(确认评价)
     * 终态无出口。
     */
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            PENDING, Set.of(ASSIGNED, REJECTED, CANCELLED),
            ASSIGNED, Set.of(PROCESSING, PENDING, ASSIGNED, CANCELLED),
            PROCESSING, Set.of(COMPLETED, ASSIGNED),
            COMPLETED, Set.of(CONFIRMED),
            CONFIRMED, Set.of(),
            CANCELLED, Set.of(),
            REJECTED, Set.of()
    );

    private OrderStatus() {
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
}
