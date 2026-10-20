package com.repair.common.enums;

import com.repair.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工单状态机测试：design.md §1.3 全部合法跳转 + 非法跳转拦截 + 终态。
 */
class OrderStatusTest {

    @Test
    void allLegalTransitions() {
        assertDoesNotThrow(() -> OrderStatus.requireTransition(OrderStatus.PENDING, OrderStatus.ASSIGNED));
        assertDoesNotThrow(() -> OrderStatus.requireTransition(OrderStatus.PENDING, OrderStatus.REJECTED));
        assertDoesNotThrow(() -> OrderStatus.requireTransition(OrderStatus.PENDING, OrderStatus.CANCELLED));
        assertDoesNotThrow(() -> OrderStatus.requireTransition(OrderStatus.ASSIGNED, OrderStatus.PROCESSING));
        assertDoesNotThrow(() -> OrderStatus.requireTransition(OrderStatus.ASSIGNED, OrderStatus.PENDING));
        assertDoesNotThrow(() -> OrderStatus.requireTransition(OrderStatus.ASSIGNED, OrderStatus.ASSIGNED));
        assertDoesNotThrow(() -> OrderStatus.requireTransition(OrderStatus.ASSIGNED, OrderStatus.CANCELLED));
        assertDoesNotThrow(() -> OrderStatus.requireTransition(OrderStatus.PROCESSING, OrderStatus.COMPLETED));
        assertDoesNotThrow(() -> OrderStatus.requireTransition(OrderStatus.PROCESSING, OrderStatus.ASSIGNED));
        assertDoesNotThrow(() -> OrderStatus.requireTransition(OrderStatus.COMPLETED, OrderStatus.CONFIRMED));
    }

    @Test
    void illegalTransitionsThrow() {
        // 没接单不能直接完成：PENDING/ASSIGNED -> COMPLETED 非法
        assertThrows(BusinessException.class, () -> OrderStatus.requireTransition(OrderStatus.PENDING, OrderStatus.COMPLETED));
        assertThrows(BusinessException.class, () -> OrderStatus.requireTransition(OrderStatus.ASSIGNED, OrderStatus.COMPLETED));
        // 未派单不能接单
        assertThrows(BusinessException.class, () -> OrderStatus.requireTransition(OrderStatus.PENDING, OrderStatus.PROCESSING));
        // 未完成不能确认
        assertThrows(BusinessException.class, () -> OrderStatus.requireTransition(OrderStatus.PENDING, OrderStatus.CONFIRMED));
        assertThrows(BusinessException.class, () -> OrderStatus.requireTransition(OrderStatus.ASSIGNED, OrderStatus.CONFIRMED));
        assertThrows(BusinessException.class, () -> OrderStatus.requireTransition(OrderStatus.PROCESSING, OrderStatus.CONFIRMED));
        // 已完成不能取消 / 驳回
        assertThrows(BusinessException.class, () -> OrderStatus.requireTransition(OrderStatus.COMPLETED, OrderStatus.CANCELLED));
        assertThrows(BusinessException.class, () -> OrderStatus.requireTransition(OrderStatus.COMPLETED, OrderStatus.REJECTED));
        // 维修中不能取消
        assertThrows(BusinessException.class, () -> OrderStatus.requireTransition(OrderStatus.PROCESSING, OrderStatus.CANCELLED));
        // 派单状态不能驳回（驳回仅限 PENDING）
        assertThrows(BusinessException.class, () -> OrderStatus.requireTransition(OrderStatus.ASSIGNED, OrderStatus.REJECTED));
    }

    @Test
    void terminalStatesHaveNoExit() {
        assertTrue(OrderStatus.isTerminal(OrderStatus.CONFIRMED));
        assertTrue(OrderStatus.isTerminal(OrderStatus.CANCELLED));
        assertTrue(OrderStatus.isTerminal(OrderStatus.REJECTED));
        assertFalse(OrderStatus.isTerminal(OrderStatus.PENDING));
        assertFalse(OrderStatus.isTerminal(OrderStatus.PROCESSING));

        // 终态不允许任何跳转
        assertThrows(BusinessException.class, () -> OrderStatus.requireTransition(OrderStatus.CONFIRMED, OrderStatus.PENDING));
        assertThrows(BusinessException.class, () -> OrderStatus.requireTransition(OrderStatus.CANCELLED, OrderStatus.PENDING));
        assertThrows(BusinessException.class, () -> OrderStatus.requireTransition(OrderStatus.REJECTED, OrderStatus.ASSIGNED));
    }

    @Test
    void unknownStatusRejected() {
        assertFalse(OrderStatus.isValid("FOO"));
        assertThrows(BusinessException.class, () -> OrderStatus.requireTransition("FOO", OrderStatus.PENDING));
        // 跳转到未知状态同样非法
        assertThrows(BusinessException.class, () -> OrderStatus.requireTransition(OrderStatus.PENDING, "FOO"));
    }

    @Test
    void errorCodeIsStatusInvalid() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> OrderStatus.requireTransition(OrderStatus.PENDING, OrderStatus.COMPLETED));
        assertEquals(400, e.getCode());
    }
}
