package com.repair.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.repair.common.enums.NotificationType;
import com.repair.common.enums.OrderStatus;
import com.repair.common.enums.RoleEnum;
import com.repair.common.exception.BusinessException;
import com.repair.common.result.ErrorCode;
import com.repair.common.result.PageResult;
import com.repair.common.util.OrderNoUtil;
import com.repair.dto.ApplyRequest;
import com.repair.dto.AssignRequest;
import com.repair.dto.AuditRequest;
import com.repair.dto.CompleteRequest;
import com.repair.dto.ConfirmRequest;
import com.repair.dto.CreateOrderRequest;
import com.repair.dto.OrderDetailVO;
import com.repair.dto.OrderExportDTO;
import com.repair.dto.OrderQuery;
import com.repair.dto.OrderVO;
import com.repair.dto.ReassignRequest;
import com.repair.dto.ReasonRequest;
import com.repair.dto.RepairerStatsVO;
import com.repair.dto.UpdateOrderRequest;
import com.repair.entity.Category;
import com.repair.entity.OrderLog;
import com.repair.entity.RepairOrder;
import com.repair.entity.User;
import com.repair.mapper.OrderLogMapper;
import com.repair.mapper.RepairOrderMapper;
import com.repair.mapper.UserMapper;
import com.repair.security.LoginUser;
import com.repair.security.SecurityUtil;
import com.repair.service.CategoryService;
import com.repair.service.NotifyService;
import com.repair.service.OrderService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 工单核心业务实现（design.md §1 完整状态机）。
 * 每个状态流转方法：@Transactional → 行锁读工单 → 状态机校验 → 条件更新工单
 * → 写 order_log → 写 notification，任一步失败全部回滚。
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final RepairOrderMapper repairOrderMapper;

    private final OrderLogMapper orderLogMapper;

    private final UserMapper userMapper;

    private final CategoryService categoryService;

    private final NotifyService notifyService;

    /** 导出创建时间格式（与 JacksonConfig 的 yyyy-MM-dd HH:mm:ss 保持一致） */
    private static final DateTimeFormatter EXPORT_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== 报修端 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(CreateOrderRequest request) {
        LoginUser current = SecurityUtil.getCurrentUser();
        Category category = categoryService.getEnabledById(request.getCategoryId());

        RepairOrder order = new RepairOrder();
        order.setOrderNo(OrderNoUtil.generate());
        order.setReporterId(current.getUserId());
        order.setCategoryId(category.getId());
        order.setTitle(request.getTitle());
        order.setDescription(request.getDescription());
        order.setImages(request.getImages());
        order.setAddress(request.getAddress());
        order.setContactPhone(request.getContactPhone());
        order.setStatus(OrderStatus.PENDING);
        order.setRemindCount(0);
        repairOrderMapper.insert(order);

        recordLog(order.getId(), current, "SUBMIT", null, OrderStatus.PENDING, "提交报修");

        // 通知所有启用中的管理员
        List<Long> adminIds = listEnabledUserIds(RoleEnum.ADMIN);
        notifyService.notifyUsers(adminIds, NotificationType.ORDER,
                "新报修工单待受理",
                "《" + order.getTitle() + "》已提交，单号：" + order.getOrderNo() + "，请及时受理。",
                order.getId());
        return order.getId();
    }

    @Override
    public PageResult<OrderVO> myOrders(Page<?> page, String status) {
        LoginUser current = SecurityUtil.getCurrentUser();
        // 支持逗号分隔多状态查询（如 PENDING,ASSIGNED,PROCESSING），底层生成 status IN (...)
        List<String> statuses = OrderStatus.parseCsv(status);
        LambdaQueryWrapper<RepairOrder> wrapper = new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getReporterId, current.getUserId())
                .in(statuses != null && !statuses.isEmpty(), RepairOrder::getStatus, statuses)
                .orderByDesc(RepairOrder::getCreatedAt);
        Page<RepairOrder> result = repairOrderMapper.selectPage(new Page<>(page.getCurrent(), page.getSize()), wrapper);
        return PageResult.from(result.convert(this::toVO));
    }

    @Override
    public OrderDetailVO detail(Long id) {
        LoginUser current = SecurityUtil.getCurrentUser();
        RepairOrder order = getOrder(id);
        requireViewPermission(order, current);

        User reporter = userMapper.selectById(order.getReporterId());
        String categoryName = categoryName(order.getCategoryId());
        User repairer = order.getRepairerId() == null ? null : userMapper.selectById(order.getRepairerId());

        List<OrderLog> logs = orderLogMapper.selectList(new LambdaQueryWrapper<OrderLog>()
                .eq(OrderLog::getOrderId, order.getId())
                .orderByAsc(OrderLog::getCreatedAt));
        Map<Long, String> operatorNames = userNames(logs.stream()
                .map(OrderLog::getOperatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        List<OrderDetailVO.OrderLogVO> logVOs = logs.stream()
                .map(log -> OrderDetailVO.OrderLogVO.from(log, operatorNames.get(log.getOperatorId())))
                .toList();

        return OrderDetailVO.from(order,
                reporter == null ? null : displayName(reporter),
                categoryName,
                repairer == null ? null : displayName(repairer),
                repairer == null ? null : repairer.getPhone(),
                logVOs);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelByReporter(Long id, ReasonRequest request) {
        LoginUser current = SecurityUtil.getCurrentUser();
        RepairOrder order = lockOrder(id);
        requireReporter(order, current);
        OrderStatus.requireTransition(order.getStatus(), OrderStatus.CANCELLED);

        UpdateWrapper<RepairOrder> wrapper = new UpdateWrapper<RepairOrder>()
                .set("cancel_reason", request.getReason());
        transition(order, OrderStatus.CANCELLED, wrapper);
        recordLog(order.getId(), current, "CANCEL", OrderStatus.PENDING, OrderStatus.CANCELLED,
                "报修人取消：" + request.getReason());

        notifyService.notifyUsers(listEnabledUserIds(RoleEnum.ADMIN), NotificationType.ORDER,
                "工单已取消",
                "《" + order.getTitle() + "》已被报修人取消（单号：" + order.getOrderNo() + "），原因："
                        + request.getReason() + "。",
                order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UpdateOrderRequest request) {
        LoginUser current = SecurityUtil.getCurrentUser();
        RepairOrder order = lockOrder(id);
        requireReporter(order, current);
        // 修改不改变状态：仅 PENDING 可改（PENDING→PENDING 不在状态机内，故直接守卫）
        if (!OrderStatus.PENDING.equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        Category category = categoryService.getEnabledById(request.getCategoryId());

        int rows = repairOrderMapper.update(null, new UpdateWrapper<RepairOrder>()
                .eq("id", order.getId())
                .eq("status", OrderStatus.PENDING)
                .eq("deleted", 0)
                .set("category_id", category.getId())
                .set("title", request.getTitle())
                .set("description", request.getDescription())
                .set("images", request.getImages())
                .set("address", request.getAddress())
                .set("contact_phone", request.getContactPhone())
                .set("updated_at", LocalDateTime.now()));
        if (rows != 1) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }

        recordLog(order.getId(), current, "UPDATE", OrderStatus.PENDING, OrderStatus.PENDING, "报修人修改工单");

        notifyService.notifyUsers(listEnabledUserIds(RoleEnum.ADMIN), NotificationType.ORDER,
                "报修工单已修改",
                "《" + request.getTitle() + "》已被报修人修改（单号：" + order.getOrderNo() + "）。",
                order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id, ConfirmRequest request) {
        LoginUser current = SecurityUtil.getCurrentUser();
        RepairOrder order = lockOrder(id);
        requireReporter(order, current);
        OrderStatus.requireTransition(order.getStatus(), OrderStatus.CONFIRMED);

        UpdateWrapper<RepairOrder> wrapper = new UpdateWrapper<RepairOrder>()
                .set("rating", request.getRating())
                .set("rating_comment", request.getRatingComment())
                .set("confirm_time", LocalDateTime.now());
        transition(order, OrderStatus.CONFIRMED, wrapper);
        recordLog(order.getId(), current, "CONFIRM", OrderStatus.COMPLETED, OrderStatus.CONFIRMED,
                "报修人确认并评价：" + request.getRating() + " 分");

        // 更新师傅聚合数据（行锁防并发丢失更新）
        if (order.getRepairerId() != null) {
            User repairer = userMapper.selectByIdForUpdate(order.getRepairerId());
            if (repairer != null) {
                BigDecimal avg = avgRating(order.getRepairerId());
                long finishCount = countByStatus(order.getRepairerId(), OrderStatus.CONFIRMED);
                userMapper.update(null, new UpdateWrapper<User>()
                        .eq("id", repairer.getId())
                        .set("rating", avg)
                        .set("finish_count", finishCount));
            }
        }

        notifyService.notify(order.getRepairerId(), NotificationType.ORDER,
                "获得新评价",
                "《" + order.getTitle() + "》获得 " + request.getRating() + " 分评价（单号："
                        + order.getOrderNo() + "）。",
                order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remind(Long id) {
        LoginUser current = SecurityUtil.getCurrentUser();
        RepairOrder order = lockOrder(id);
        requireReporter(order, current);
        if (!OrderStatus.PROCESSING.equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        if (order.getRemindCount() != null && order.getRemindCount() >= 3) {
            throw new BusinessException(ErrorCode.REMIND_TOO_FREQUENT);
        }
        // 30 分钟内已催过则拒绝
        List<OrderLog> lastRemind = orderLogMapper.selectList(new LambdaQueryWrapper<OrderLog>()
                .eq(OrderLog::getOrderId, order.getId())
                .eq(OrderLog::getAction, "REMIND")
                .orderByDesc(OrderLog::getCreatedAt)
                .last("LIMIT 1"));
        if (!lastRemind.isEmpty() && lastRemind.get(0).getCreatedAt() != null
                && lastRemind.get(0).getCreatedAt().plusMinutes(30).isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.REMIND_TOO_FREQUENT);
        }

        int rows = repairOrderMapper.update(null, new UpdateWrapper<RepairOrder>()
                .eq("id", order.getId())
                .eq("status", OrderStatus.PROCESSING)
                .eq("deleted", 0)
                .setSql("remind_count = remind_count + 1")
                .set("updated_at", LocalDateTime.now()));
        if (rows != 1) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }

        recordLog(order.getId(), current, "REMIND", OrderStatus.PROCESSING, OrderStatus.PROCESSING, "报修人催单");
        notifyService.notify(order.getRepairerId(), NotificationType.ORDER,
                "报修人催单",
                "报修人催办《" + order.getTitle() + "》（单号：" + order.getOrderNo() + "）。",
                order.getId());
    }

    // ==================== 维修端 ====================

    @Override
    public PageResult<OrderVO> repairerOrders(Page<?> page, String status) {
        LoginUser current = SecurityUtil.getCurrentUser();
        // 支持逗号分隔多状态查询（如 REASSIGNING,ARBITRATING），底层生成 status IN (...)
        List<String> statuses = OrderStatus.parseCsv(status);
        LambdaQueryWrapper<RepairOrder> wrapper = new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getRepairerId, current.getUserId())
                .in(statuses != null && !statuses.isEmpty(), RepairOrder::getStatus, statuses)
                .orderByDesc(RepairOrder::getCreatedAt);
        Page<RepairOrder> result = repairOrderMapper.selectPage(new Page<>(page.getCurrent(), page.getSize()), wrapper);
        return PageResult.from(result.convert(this::toVO));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void accept(Long id) {
        LoginUser current = SecurityUtil.getCurrentUser();
        RepairOrder order = lockOrder(id);
        requireRepairerOfOrder(order, current);
        OrderStatus.requireTransition(order.getStatus(), OrderStatus.PROCESSING);

        transition(order, OrderStatus.PROCESSING,
                new UpdateWrapper<RepairOrder>().set("accept_time", LocalDateTime.now()));
        recordLog(order.getId(), current, "ACCEPT", OrderStatus.ASSIGNED, OrderStatus.PROCESSING, "师傅接单");

        User repairer = userMapper.selectById(current.getUserId());
        notifyService.notify(order.getReporterId(), NotificationType.ORDER,
                "维修师傅已接单",
                "《" + order.getTitle() + "》已由师傅 " + displayName(repairer)
                        + (StringUtils.hasText(repairer.getPhone()) ? "（电话 " + repairer.getPhone() + "）" : "")
                        + " 接单，开始维修（单号：" + order.getOrderNo() + "）。",
                order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reassignRequest(Long id, ApplyRequest request) {
        LoginUser current = SecurityUtil.getCurrentUser();
        RepairOrder order = lockOrder(id);
        requireRepairerOfOrder(order, current);
        OrderStatus.requireTransition(order.getStatus(), OrderStatus.REASSIGNING);

        transition(order, OrderStatus.REASSIGNING,
                new UpdateWrapper<RepairOrder>()
                        .set("apply_reason", request.getReason())
                        .set("apply_images", request.getApplyImages()));
        recordLog(order.getId(), current, "REASSIGN_REQUEST", OrderStatus.ASSIGNED, OrderStatus.REASSIGNING,
                "师傅申请改派：" + request.getReason());

        notifyService.notifyUsers(listEnabledUserIds(RoleEnum.ADMIN), NotificationType.ORDER,
                "师傅申请改派",
                "《" + order.getTitle() + "》的师傅申请改派（单号：" + order.getOrderNo() + "），原因："
                        + request.getReason() + "，请审批。",
                order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void arbitrationRequest(Long id, ApplyRequest request) {
        LoginUser current = SecurityUtil.getCurrentUser();
        RepairOrder order = lockOrder(id);
        requireRepairerOfOrder(order, current);
        OrderStatus.requireTransition(order.getStatus(), OrderStatus.ARBITRATING);

        transition(order, OrderStatus.ARBITRATING,
                new UpdateWrapper<RepairOrder>()
                        .set("apply_reason", request.getReason())
                        .set("apply_images", request.getApplyImages()));
        recordLog(order.getId(), current, "ARBITRATE_REQUEST", OrderStatus.PROCESSING, OrderStatus.ARBITRATING,
                "师傅申请仲裁（恶意报修）：" + request.getReason());

        notifyService.notifyUsers(listEnabledUserIds(RoleEnum.ADMIN), NotificationType.ORDER,
                "师傅申请仲裁",
                "《" + order.getTitle() + "》的师傅申请仲裁（单号：" + order.getOrderNo() + "），原因："
                        + request.getReason() + "，请审批。",
                order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long id, CompleteRequest request) {
        LoginUser current = SecurityUtil.getCurrentUser();
        RepairOrder order = lockOrder(id);
        requireRepairerOfOrder(order, current);
        OrderStatus.requireTransition(order.getStatus(), OrderStatus.COMPLETED);

        // 维修结果描述为选填，未填写时存默认文案，避免日志与详情出现空值
        String repairResult = request.getRepairResult() == null || request.getRepairResult().isBlank()
                ? "已维修完成"
                : request.getRepairResult().trim();

        transition(order, OrderStatus.COMPLETED,
                new UpdateWrapper<RepairOrder>()
                        .set("repair_result", repairResult)
                        .set("finish_time", LocalDateTime.now()));
        recordLog(order.getId(), current, "FINISH", OrderStatus.PROCESSING, OrderStatus.COMPLETED,
                "维修完成：" + repairResult);

        notifyService.notify(order.getReporterId(), NotificationType.ORDER,
                "维修已完成，待确认",
                "《" + order.getTitle() + "》已维修完成（单号：" + order.getOrderNo() + "），请确认并评价。",
                order.getId());
    }

    @Override
    public RepairerStatsVO repairerStats() {
        LoginUser current = SecurityUtil.getCurrentUser();
        User user = userMapper.selectById(current.getUserId());
        long processing = countByStatus(current.getUserId(), OrderStatus.PROCESSING);
        long assigned = countByStatus(current.getUserId(), OrderStatus.ASSIGNED);
        return RepairerStatsVO.builder()
                .finishCount(user == null || user.getFinishCount() == null ? 0 : user.getFinishCount())
                .rating(user == null ? null : user.getRating())
                .processingCount(processing)
                .assignedCount(assigned)
                .build();
    }

    // ==================== 管理端 ====================

    @Override
    public PageResult<OrderVO> adminOrders(Page<?> page, OrderQuery query) {
        LambdaQueryWrapper<RepairOrder> wrapper = buildAdminQueryWrapper(query);
        Page<RepairOrder> result = repairOrderMapper.selectPage(new Page<>(page.getCurrent(), page.getSize()), wrapper);
        return PageResult.from(result.convert(this::toVO));
    }

    @Override
    public void exportOrders(HttpServletResponse response, OrderQuery query) {
        List<RepairOrder> orders = repairOrderMapper.selectList(buildAdminQueryWrapper(query));
        List<OrderExportDTO> exportList = orders.stream().map(this::toExportDTO).toList();
        try {
            EasyExcel.write(response.getOutputStream(), OrderExportDTO.class)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .sheet("工单数据")
                    .doWrite(exportList);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "工单导出失败");
        }
    }

    /**
     * 构建管理员工单查询条件（关键字/状态/分类/时间）。
     * adminOrders 与 exportOrders 共用，避免两处筛选逻辑漂移。
     */
    private LambdaQueryWrapper<RepairOrder> buildAdminQueryWrapper(OrderQuery query) {
        String keyword = query.getKeyword();
        // 按报修人姓名/账号匹配：先查报修人主键，再 in 过滤（避免 SQL 拼接注入）。
        // 无匹配报修人时使用空集合，保证 keyword 仅命中报修人字段时结果为空，而非回退全量。
        boolean hasKeyword = StringUtils.hasText(keyword);
        List<Long> reporterIds = hasKeyword ? userMapper.selectReporterIdsByKeyword(keyword.trim()) : null;

        // 状态过滤：忽略 null、空白与哨兵值 "ALL"（前端「全部」Tab 可能误传）。
        // 支持逗号分隔多状态（如 PENDING,REASSIGNING,ARBITRATING），底层生成 status IN (...)。
        String status = query.getStatus();
        List<String> statuses = status == null || status.isBlank() || "ALL".equalsIgnoreCase(status.trim())
                ? null
                : OrderStatus.parseCsv(status);
        boolean hasStatus = statuses != null && !statuses.isEmpty();

        return new LambdaQueryWrapper<RepairOrder>()
                .in(hasStatus, RepairOrder::getStatus, statuses)
                .eq(query.getCategoryId() != null, RepairOrder::getCategoryId, query.getCategoryId())
                .ge(query.getStartTime() != null, RepairOrder::getCreatedAt, query.getStartTime())
                .le(query.getEndTime() != null, RepairOrder::getCreatedAt, query.getEndTime())
                .and(hasKeyword, w -> {
                    w.like(RepairOrder::getOrderNo, keyword)
                            .or()
                            .like(RepairOrder::getTitle, keyword);
                    if (reporterIds != null && !reporterIds.isEmpty()) {
                        w.or().in(RepairOrder::getReporterId, reporterIds);
                    }
                })
                .orderByDesc(RepairOrder::getCreatedAt);
    }

    @Override
    public long pendingCount() {
        return repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getStatus, OrderStatus.PENDING));
    }

    @Override
    public long auditingCount() {
        return repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                .in(RepairOrder::getStatus, OrderStatus.REASSIGNING, OrderStatus.ARBITRATING));
    }

    @Override
    public PageResult<OrderVO> auditingOrders(Page<?> page, String type) {
        // type 合法值：REASSIGN / ARBITRATE；空或非法一律查全部待审批
        boolean reassign = "REASSIGN".equalsIgnoreCase(type == null ? "" : type.trim());
        boolean arbitrate = "ARBITRATE".equalsIgnoreCase(type == null ? "" : type.trim());
        LambdaQueryWrapper<RepairOrder> wrapper = new LambdaQueryWrapper<RepairOrder>()
                .orderByDesc(RepairOrder::getCreatedAt);
        if (reassign) {
            wrapper.eq(RepairOrder::getStatus, OrderStatus.REASSIGNING);
        } else if (arbitrate) {
            wrapper.eq(RepairOrder::getStatus, OrderStatus.ARBITRATING);
        } else {
            wrapper.in(RepairOrder::getStatus, OrderStatus.REASSIGNING, OrderStatus.ARBITRATING);
        }
        Page<RepairOrder> result = repairOrderMapper.selectPage(new Page<>(page.getCurrent(), page.getSize()), wrapper);
        return PageResult.from(result.convert(this::toVO));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, AuditRequest request) {
        LoginUser current = SecurityUtil.getCurrentUser();
        RepairOrder order = lockOrder(id);
        String type = request.getType() == null ? "" : request.getType().trim();
        boolean approve = Boolean.TRUE.equals(request.getApprove());
        String remark = StringUtils.hasText(request.getRemark()) ? request.getRemark().trim() : "";

        if ("ARBITRATE".equalsIgnoreCase(type)) {
            auditArbitrate(order, current, approve, remark);
        } else if ("REASSIGN".equalsIgnoreCase(type)) {
            auditReassign(order, current, approve, remark);
        } else {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "审批类型非法，仅支持 REASSIGN / ARBITRATE");
        }
    }

    /** 仲裁审批：同意 → REJECTED 并扣报修人信誉分；驳回 → PROCESSING 打回重修 */
    private void auditArbitrate(RepairOrder order, LoginUser current, boolean approve, String remark) {
        if (approve) {
            OrderStatus.requireTransition(order.getStatus(), OrderStatus.REJECTED);
            transition(order, OrderStatus.REJECTED,
                    new UpdateWrapper<RepairOrder>().set("reject_reason", order.getApplyReason()));
            recordLog(order.getId(), current, "AUDIT", OrderStatus.ARBITRATING, OrderStatus.REJECTED,
                    "仲裁通过，驳回工单" + (remark.isEmpty() ? "" : "（备注：" + remark + "）"));

            // 扣减报修人信誉分，低于 60 自动禁用（同一事务，行锁防并发丢更新）
            deductReporterCredit(order.getReporterId());

            notifyService.notify(order.getRepairerId(), NotificationType.ORDER,
                    "仲裁已通过",
                    "《" + order.getTitle() + "》仲裁通过，工单已驳回（单号：" + order.getOrderNo() + "）。",
                    order.getId());
            notifyService.notify(order.getReporterId(), NotificationType.SYSTEM,
                    "报修被驳回，信誉分已扣减",
                    "您的报修《" + order.getTitle() + "》经仲裁被驳回（单号：" + order.getOrderNo()
                            + "），信誉分已扣减 10 分。",
                    order.getId());
        } else {
            OrderStatus.requireTransition(order.getStatus(), OrderStatus.PROCESSING);
            transition(order, OrderStatus.PROCESSING, new UpdateWrapper<>());
            recordLog(order.getId(), current, "AUDIT", OrderStatus.ARBITRATING, OrderStatus.PROCESSING,
                    "仲裁驳回，打回重修" + (remark.isEmpty() ? "" : "（备注：" + remark + "）"));

            notifyService.notify(order.getRepairerId(), NotificationType.ORDER,
                    "仲裁未通过，请继续维修",
                    "《" + order.getTitle() + "》的仲裁申请未通过（单号：" + order.getOrderNo() + "），请继续维修。",
                    order.getId());
        }
    }

    /** 改派审批：同意 → PENDING 重新派单；驳回 → ASSIGNED 继续接单 */
    private void auditReassign(RepairOrder order, LoginUser current, boolean approve, String remark) {
        if (approve) {
            OrderStatus.requireTransition(order.getStatus(), OrderStatus.PENDING);
            transition(order, OrderStatus.PENDING,
                    new UpdateWrapper<RepairOrder>()
                            .set("repairer_id", null)
                            .set("assign_time", null)
                            .set("accept_time", null));
            recordLog(order.getId(), current, "AUDIT", OrderStatus.REASSIGNING, OrderStatus.PENDING,
                    "改派通过，重新派单" + (remark.isEmpty() ? "" : "（备注：" + remark + "）"));

            notifyService.notify(order.getRepairerId(), NotificationType.ORDER,
                    "改派申请已通过",
                    "《" + order.getTitle() + "》的改派申请已通过（单号：" + order.getOrderNo() + "），无需继续处理。",
                    order.getId());
            notifyService.notifyUsers(listEnabledUserIds(RoleEnum.ADMIN), NotificationType.ORDER,
                    "改派已通过，待重新派单",
                    "您已通过改派申请，请重新指派维修师傅。",
                    order.getId());
        } else {
            OrderStatus.requireTransition(order.getStatus(), OrderStatus.ASSIGNED);
            transition(order, OrderStatus.ASSIGNED, new UpdateWrapper<>());
            recordLog(order.getId(), current, "AUDIT", OrderStatus.REASSIGNING, OrderStatus.ASSIGNED,
                    "改派驳回，继续接单" + (remark.isEmpty() ? "" : "（备注：" + remark + "）"));

            notifyService.notify(order.getRepairerId(), NotificationType.ORDER,
                    "改派申请未通过",
                    "《" + order.getTitle() + "》的改派申请未通过（单号：" + order.getOrderNo() + "），请继续接单。",
                    order.getId());
        }
    }

    /** 扣减报修人信誉分：行锁读取 → 减 10 → 低于 60 禁用（同一事务） */
    private void deductReporterCredit(Long reporterId) {
        if (reporterId == null) {
            return;
        }
        User reporter = userMapper.selectByIdForUpdate(reporterId);
        if (reporter == null) {
            return;
        }
        int current = reporter.getCreditScore() == null ? 100 : reporter.getCreditScore();
        int newScore = current - 10;
        UpdateWrapper<User> wrapper = new UpdateWrapper<User>()
                .eq("id", reporterId)
                .set("credit_score", newScore);
        if (newScore < 60) {
            wrapper.set("status", 0);
        }
        userMapper.update(null, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assign(Long id, AssignRequest request) {
        LoginUser current = SecurityUtil.getCurrentUser();
        RepairOrder order = lockOrder(id);
        OrderStatus.requireTransition(order.getStatus(), OrderStatus.ASSIGNED);
        User repairer = requireRepairer(request.getRepairerId());

        transition(order, OrderStatus.ASSIGNED,
                new UpdateWrapper<RepairOrder>()
                        .set("repairer_id", repairer.getId())
                        .set("assign_time", LocalDateTime.now()));
        recordLog(order.getId(), current, "ASSIGN", OrderStatus.PENDING, OrderStatus.ASSIGNED,
                "派单给师傅：" + displayName(repairer));

        notifyService.notify(repairer.getId(), NotificationType.ORDER,
                "新工单待接单",
                "《" + order.getTitle() + "》已派单给您（单号：" + order.getOrderNo() + "）"
                        + (StringUtils.hasText(order.getAddress()) ? "，地址：" + order.getAddress() : "") + "。",
                order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reassign(Long id, ReassignRequest request) {
        LoginUser current = SecurityUtil.getCurrentUser();
        RepairOrder order = lockOrder(id);
        OrderStatus.requireTransition(order.getStatus(), OrderStatus.ASSIGNED);
        if (order.getRepairerId() == null) {
            // 拒单回到 PENDING 后走 assign 而非 reassign；此处防御
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        if (order.getRepairerId().equals(request.getRepairerId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新师傅不能与现任师傅相同");
        }
        User newRepairer = requireRepairer(request.getRepairerId());
        Long oldRepairerId = order.getRepairerId();
        String fromStatus = order.getStatus();

        transition(order, OrderStatus.ASSIGNED,
                new UpdateWrapper<RepairOrder>()
                        .set("repairer_id", newRepairer.getId())
                        .set("assign_time", LocalDateTime.now())
                        .set("accept_time", null));
        recordLog(order.getId(), current, "ASSIGN", fromStatus, OrderStatus.ASSIGNED,
                "改派：由师傅 " + (oldRepairerId == null ? "-" : displayName(userMapper.selectById(oldRepairerId)))
                        + " 改为 " + displayName(newRepairer));

        notifyService.notify(newRepairer.getId(), NotificationType.ORDER,
                "新工单待接单",
                "《" + order.getTitle() + "》已派单给您（单号：" + order.getOrderNo() + "）。",
                order.getId());
        notifyService.notify(oldRepairerId, NotificationType.ORDER,
                "工单已改派",
                "《" + order.getTitle() + "》已改派给其他师傅（单号：" + order.getOrderNo() + "），无需继续处理。",
                order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectByAdmin(Long id, ReasonRequest request) {
        LoginUser current = SecurityUtil.getCurrentUser();
        RepairOrder order = lockOrder(id);
        OrderStatus.requireTransition(order.getStatus(), OrderStatus.REJECTED);

        transition(order, OrderStatus.REJECTED,
                new UpdateWrapper<RepairOrder>().set("reject_reason", request.getReason()));
        recordLog(order.getId(), current, "REJECT", OrderStatus.PENDING, OrderStatus.REJECTED,
                "管理员驳回：" + request.getReason());

        notifyService.notify(order.getReporterId(), NotificationType.ORDER,
                "报修单被驳回",
                "《" + order.getTitle() + "》被驳回（单号：" + order.getOrderNo() + "），原因：" + request.getReason() + "。",
                order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelByAdmin(Long id, ReasonRequest request) {
        LoginUser current = SecurityUtil.getCurrentUser();
        RepairOrder order = lockOrder(id);
        OrderStatus.requireTransition(order.getStatus(), OrderStatus.CANCELLED);
        String fromStatus = order.getStatus();

        transition(order, OrderStatus.CANCELLED,
                new UpdateWrapper<RepairOrder>().set("cancel_reason", request.getReason()));
        recordLog(order.getId(), current, "CANCEL", fromStatus, OrderStatus.CANCELLED,
                "管理员取消：" + request.getReason());

        notifyService.notify(order.getReporterId(), NotificationType.ORDER,
                "工单已被取消",
                "《" + order.getTitle() + "》已被管理员取消（单号：" + order.getOrderNo() + "），原因："
                        + request.getReason() + "。",
                order.getId());
        if (order.getRepairerId() != null) {
            notifyService.notify(order.getRepairerId(), NotificationType.ORDER,
                    "工单已被取消",
                    "您负责的《" + order.getTitle() + "》已被管理员取消（单号：" + order.getOrderNo() + "）。",
                    order.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByAdmin(Long id) {
        RepairOrder order = lockOrder(id);
        // 仅「待派单（PENDING）」「已驳回（REJECTED）」「已取消（CANCELLED）」可删除；
        // 已派单、维修中、已完成等流转中状态一律拒绝
        if (!OrderStatus.PENDING.equals(order.getStatus())
                && !OrderStatus.CANCELLED.equals(order.getStatus())
                && !OrderStatus.REJECTED.equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "流转中的工单无法删除，请先取消或驳回");
        }
        // 逻辑删除：RepairOrder 上的 @TableLogic 注解会让 deleteById 执行 UPDATE deleted=1，不物理删除
        repairOrderMapper.deleteById(id);
    }

    // ==================== 内部工具方法 ====================

    /** 普通读取（无锁） */
    private RepairOrder getOrder(Long id) {
        RepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    /** 行锁读取：状态流转入口，防并发重复操作 */
    private RepairOrder lockOrder(Long id) {
        RepairOrder order = repairOrderMapper.selectByIdForUpdate(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    /**
     * 条件更新工单状态（extra 为额外的 set 列）。
     * 带 status=当前值 条件：即使行锁已持有，条件更新仍是第二道保险，
     * 影响行数 != 1 即抛「当前状态不允许该操作」。
     */
    private void transition(RepairOrder order, String target, UpdateWrapper<RepairOrder> extra) {
        UpdateWrapper<RepairOrder> wrapper = extra == null ? new UpdateWrapper<>() : extra;
        wrapper.eq("id", order.getId())
                .eq("status", order.getStatus())
                .eq("deleted", 0)
                .set("status", target)
                .set("updated_at", LocalDateTime.now());
        int rows = repairOrderMapper.update(null, wrapper);
        if (rows != 1) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        order.setStatus(target);
    }

    private void recordLog(Long orderId, LoginUser operator, String action,
                           String fromStatus, String toStatus, String remark) {
        OrderLog log = new OrderLog();
        log.setOrderId(orderId);
        log.setOperatorId(operator.getUserId());
        log.setOperatorRole(operator.getRole());
        log.setAction(action);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setRemark(remark);
        orderLogMapper.insert(log);
    }

    /** 校验操作人是工单报修人（非本人 403） */
    private void requireReporter(RepairOrder order, LoginUser current) {
        if (!order.getReporterId().equals(current.getUserId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_OWNED);
        }
    }

    /** 校验操作人是被指派的师傅（非本人 403） */
    private void requireRepairerOfOrder(RepairOrder order, LoginUser current) {
        if (order.getRepairerId() == null || !order.getRepairerId().equals(current.getUserId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_OWNED);
        }
    }

    /** 详情查看权限：报修人 / 被派师傅 / 管理员 */
    private void requireViewPermission(RepairOrder order, LoginUser current) {
        boolean allowed = order.getReporterId().equals(current.getUserId())
                || (order.getRepairerId() != null && order.getRepairerId().equals(current.getUserId()))
                || RoleEnum.ADMIN.equals(current.getRole());
        if (!allowed) {
            throw new BusinessException(ErrorCode.ORDER_NOT_OWNED);
        }
    }

    /** 校验师傅存在、角色为 REPAIRER 且启用 */
    private User requireRepairer(Long repairerId) {
        User user = userMapper.selectById(repairerId);
        if (user == null || !RoleEnum.REPAIRER.equals(user.getRole())
                || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.REPAIRER_INVALID);
        }
        return user;
    }

    private List<Long> listEnabledUserIds(String role) {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRole, role)
                .eq(User::getStatus, 1));
        return users.stream().map(User::getId).toList();
    }

    private long countByStatus(Long repairerId, String status) {
        return repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getRepairerId, repairerId)
                .eq(RepairOrder::getStatus, status));
    }

    private BigDecimal avgRating(Long repairerId) {
        List<RepairOrder> rated = repairOrderMapper.selectList(new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getRepairerId, repairerId)
                .isNotNull(RepairOrder::getRating));
        if (rated.isEmpty()) {
            return null;
        }
        BigDecimal sum = rated.stream()
                .map(RepairOrder::getRating)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(rated.size()), 1, java.math.RoundingMode.HALF_UP);
    }

    private String categoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        Category category = null;
        try {
            category = categoryService.getEnabledById(categoryId);
        } catch (BusinessException ignored) {
            // 分类被停用后详情仍可展示原分类名；兜底返回 null
        }
        return category == null ? null : category.getName();
    }

    private String displayName(User user) {
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();
    }

    /** 批量查用户名映射（real_name 优先） */
    private Map<Long, String> userNames(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> distinct = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(User::getId, this::displayName, (a, b) -> a, HashMap::new));
    }

    /** 单个工单转列表 VO（批量补全名称）。注意：repairerId 可能为 null，不能直接 List.of。 */
    private OrderVO toVO(RepairOrder order) {
        Map<Long, String> users = userNames(Stream.of(order.getReporterId(), order.getRepairerId())
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        return OrderVO.from(order,
                users.get(order.getReporterId()),
                categoryName(order.getCategoryId()),
                users.get(order.getRepairerId()));
    }

    /** 单个工单转导出 DTO（状态中文映射 + 创建时间格式化；维修师傅为空则导出空单元格） */
    private OrderExportDTO toExportDTO(RepairOrder order) {
        Map<Long, String> users = userNames(Stream.of(order.getReporterId(), order.getRepairerId())
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        OrderExportDTO dto = new OrderExportDTO();
        dto.setOrderNo(order.getOrderNo());
        dto.setTitle(order.getTitle());
        dto.setCategoryName(categoryName(order.getCategoryId()));
        dto.setReporterName(users.get(order.getReporterId()));
        dto.setRepairerName(order.getRepairerId() == null ? null : users.get(order.getRepairerId()));
        dto.setStatus(OrderStatus.toLabel(order.getStatus()));
        dto.setCreatedAt(order.getCreatedAt() == null ? null : order.getCreatedAt().format(EXPORT_TIME_FORMATTER));
        return dto;
    }
}
