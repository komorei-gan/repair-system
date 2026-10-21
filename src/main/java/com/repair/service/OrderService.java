package com.repair.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.repair.common.result.PageResult;
import com.repair.dto.AssignRequest;
import com.repair.dto.CompleteRequest;
import com.repair.dto.ConfirmRequest;
import com.repair.dto.CreateOrderRequest;
import com.repair.dto.OrderDetailVO;
import com.repair.dto.OrderQuery;
import com.repair.dto.OrderVO;
import com.repair.dto.ReassignRequest;
import com.repair.dto.ReasonRequest;
import com.repair.dto.RepairerStatsVO;

/**
 * 工单核心业务（design.md §1 完整状态机，§1.3 全部 11 种操作）。
 * 所有状态流转方法在实现层标注 @Transactional：
 * 状态更新 → order_log → notification 同一事务，强一致。
 */
public interface OrderService {

    /** 报修人提交报修工单 → PENDING，通知所有启用管理员 */
    Long submit(CreateOrderRequest request);

    /** 我的报修列表（分页，按状态筛选） */
    PageResult<OrderVO> myOrders(Page<?> page, String status);

    /** 工单详情（含操作日志）；调用方需通过校验层（报修人/被派师傅/管理员） */
    OrderDetailVO detail(Long id);

    /** 报修人取消：PENDING → CANCELLED，通知管理员 */
    void cancelByReporter(Long id, ReasonRequest request);

    /** 报修人确认并评价：COMPLETED → CONFIRMED，通知师傅 */
    void confirm(Long id, ConfirmRequest request);

    /** 报修人催单：PROCESSING 状态不变，通知师傅（限频） */
    void remind(Long id);

    /** 师傅任务列表（分页，按状态筛选） */
    PageResult<OrderVO> repairerOrders(Page<?> page, String status);

    /** 师傅接单：ASSIGNED → PROCESSING，通知报修人 */
    void accept(Long id);

    /** 师傅拒单：ASSIGNED → PENDING，通知管理员 */
    void reject(Long id, ReasonRequest request);

    /** 师傅提交完成：PROCESSING → COMPLETED，通知报修人 */
    void complete(Long id, CompleteRequest request);

    /** 师傅统计 */
    RepairerStatsVO repairerStats();

    /** 管理员工单分页（状态/分类/时间/关键字） */
    PageResult<OrderVO> adminOrders(Page<?> page, OrderQuery query);

    /** 待受理（PENDING）工单数，供左侧菜单角标展示 */
    long pendingCount();

    /** 管理员派单：PENDING → ASSIGNED，通知师傅 */
    void assign(Long id, AssignRequest request);

    /** 管理员改派：ASSIGNED/PROCESSING → ASSIGNED（换人），通知新旧师傅 */
    void reassign(Long id, ReassignRequest request);

    /** 管理员驳回：PENDING → REJECTED，通知报修人 */
    void rejectByAdmin(Long id, ReasonRequest request);

    /** 管理员取消：PENDING/ASSIGNED → CANCELLED，通知报修人（有师傅则一并） */
    void cancelByAdmin(Long id, ReasonRequest request);
}
