package com.repair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.repair.common.enums.OrderStatus;
import com.repair.common.enums.RoleEnum;
import com.repair.common.util.OrderNoUtil;
import com.repair.entity.Category;
import com.repair.entity.OrderLog;
import com.repair.entity.RepairOrder;
import com.repair.entity.User;
import com.repair.mapper.CategoryMapper;
import com.repair.mapper.OrderLogMapper;
import com.repair.mapper.RepairOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 数据初始化（幂等，仅首次启动创建）：
 * 1. 默认管理员账号 admin / admin123；
 * 2. 默认故障分类：水电 / 门窗 / 网络 / 家具 / 其他；
 * 3. 两个联调用维修师傅：master / master123、master2 / master123；
 * 4. 报修人 zhangsan（不存在则自动创建）名下生成 12 条测试工单，
 *    状态覆盖全部 7 种：PENDING×2 / ASSIGNED×2 / PROCESSING×2 / COMPLETED×2 /
 *    CONFIRMED×2 / CANCELLED×1 / REJECTED×1，并为每条工单写入对应的 order_log，
 *    保证详情页操作日志（el-timeline）有内容可渲染。
 *
 * <p><b>环境隔离</b>：本类各环境的种子数据均为「不存在才创建」，可安全用于生产首次部署。
 * 其中「强制重置 zhangsan 密码」的兜底逻辑已拆分到
 * {@link DevReporterAccountResetter}，仅在 {@code dev} profile 下注册，生产环境不会执行。
 * 正式部署后请立即修改密码。
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String SEED_REPORTER = "zhangsan";

    /** 测试工单标题池（仅用于种子数据，与真实业务无关；与 statuses 一一对应） */
    private static final String[] SEED_TITLES = {
            "3 号教学楼 402 多媒体教室投影仪无法开机",
            "学生公寓 7 栋 512 卫生间水压过低",
            "图书馆 2 层自习区空调不制冷",
            "体育馆器材室西侧门锁损坏无法上锁",
            "实验楼 B 栋 305 网络端口无信号",
            "一号食堂后厨排水口堵塞返水",
            "行政楼 3 层走廊照明灯不亮",
            "操场主席台音响设备无声音",
            "2 号宿舍楼公共洗衣机故障",
            "教学楼 A 栋电梯按键失灵",
            "体育馆淋浴间热水管破裂漏水",
            "实验楼 C 栋 2 层公共打印机卡纸",
    };

    private final UserService userService;

    private final CategoryMapper categoryMapper;

    private final RepairOrderMapper repairOrderMapper;

    private final OrderLogMapper orderLogMapper;

    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initAdmin();
        initCategories();
        initRepairers();
        initSeedOrders();
        backfillMissingLogs();
    }

    private void initAdmin() {
        if (userService.existsByUsername("admin")) {
            log.info("管理员账号已存在，跳过初始化");
            return;
        }
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRealName("系统管理员");
        admin.setRole(RoleEnum.ADMIN);
        admin.setStatus(1);
        admin.setFinishCount(0);
        userService.save(admin);
        log.warn("已创建默认管理员账号 admin / admin123，正式部署请立即修改密码！");
    }

    private void initCategories() {
        if (categoryMapper.selectCount(null) > 0) {
            log.info("分类已存在，跳过初始化");
            return;
        }
        String[] names = {"水电", "门窗", "网络", "家具", "其他"};
        int sort = 1;
        for (String name : names) {
            Category category = new Category();
            category.setName(name);
            category.setSort(sort++);
            category.setStatus(1);
            categoryMapper.insert(category);
        }
        log.info("已创建默认故障分类：{}", String.join("、", names));
    }

    private void initRepairers() {
        createRepairerIfAbsent("master", "master123", "王师傅");
        createRepairerIfAbsent("master2", "master123", "李师傅");
    }

    private void createRepairerIfAbsent(String username, String rawPassword, String realName) {
        if (userService.existsByUsername(username)) {
            return;
        }
        User repairer = new User();
        repairer.setUsername(username);
        repairer.setPassword(passwordEncoder.encode(rawPassword));
        repairer.setRealName(realName);
        repairer.setRole(RoleEnum.REPAIRER);
        repairer.setStatus(1);
        repairer.setFinishCount(0);
        userService.save(repairer);
        log.warn("已创建联调师傅账号 {} / {}，正式部署请删除或修改密码！", username, rawPassword);
    }

    /**
     * 工单种子数据：报修人 zhangsan（已由 forceResetSeedReporter 保证可用）名下生成 12 条测试工单，
     * 状态覆盖全部 7 种，并为每条工单伪造对应状态流转的 order_log（时间错开）。
     * 幂等判断基于「种子标题是否已存在」：即使库里已有其他真实/手动工单，只要种子
     * 标题尚未出现，就照常生成；种子标题已存在则跳过，防止重复插入。
     * 说明：种子数据为静态插入，绕过 Service 状态机，因此 order_log / notification
     * 均由本方法直接伪造/跳过——order_log 用于详情页展示，notification 不写以
     * 避免启动阶段向管理员与师傅推送无关测试通知。
     */
    private void initSeedOrders() {
        // zhangsan 已由 forceResetSeedReporter() 保证存在且可用
        User reporter = userService.getByUsername(SEED_REPORTER);
        if (reporter == null) {
            log.warn("报修人 {} 不存在，跳过测试工单生成", SEED_REPORTER);
            return;
        }

        // 种子标题已存在即跳过，防止重复插入
        long seeded = repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                .in(RepairOrder::getTitle, List.of(SEED_TITLES)));
        if (seeded > 0) {
            log.info("测试工单种子已存在（命中 {} 条），跳过生成", seeded);
            return;
        }

        List<Category> categories = categoryMapper.selectList(null);
        if (categories == null || categories.isEmpty()) {
            log.warn("故障分类为空，跳过测试工单生成");
            return;
        }

        User admin = userService.getByUsername("admin");
        User repairer1 = userService.getByUsername("master");
        User repairer2 = userService.getByUsername("master2");
        List<Long> repairerIds = new ArrayList<>();
        if (repairer1 != null) {
            repairerIds.add(repairer1.getId());
        }
        if (repairer2 != null) {
            repairerIds.add(repairer2.getId());
        }

        // 7 种状态全覆盖，与 SEED_TITLES 一一对应
        String[] statuses = {
                OrderStatus.PENDING, OrderStatus.PENDING,
                OrderStatus.ASSIGNED, OrderStatus.ASSIGNED,
                OrderStatus.PROCESSING, OrderStatus.PROCESSING,
                OrderStatus.COMPLETED, OrderStatus.COMPLETED,
                OrderStatus.CONFIRMED, OrderStatus.CONFIRMED,
                OrderStatus.CANCELLED,
                OrderStatus.REJECTED,
        };

        ThreadLocalRandom random = ThreadLocalRandom.current();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < SEED_TITLES.length; i++) {
            String status = statuses[i];
            Category category = categories.get(random.nextInt(categories.size()));

            // PENDING/CANCELLED/REJECTED 无师傅；其余状态随机指派师傅（师傅不存在则留空）
            boolean hasRepairer = OrderStatus.ASSIGNED.equals(status)
                    || OrderStatus.PROCESSING.equals(status)
                    || OrderStatus.COMPLETED.equals(status)
                    || OrderStatus.CONFIRMED.equals(status);
            Long repairerId = hasRepairer && !repairerIds.isEmpty()
                    ? repairerIds.get(random.nextInt(repairerIds.size()))
                    : null;

            // 创建时间按索引递减错开，保证列表按时间倒序时有区分度
            LocalDateTime createdAt = now.minusMinutes((long) (SEED_TITLES.length - i) * 30);

            RepairOrder order = new RepairOrder();
            order.setOrderNo(OrderNoUtil.generate());
            order.setReporterId(reporter.getId());
            order.setCategoryId(category.getId());
            order.setTitle(SEED_TITLES[i]);
            order.setDescription(SEED_TITLES[i]);
            order.setAddress("校园内 · 详见故障描述");
            order.setContactPhone(reporter.getPhone());
            order.setStatus(status);
            order.setRemindCount(0);
            order.setRepairerId(repairerId);
            if (repairerId != null) {
                order.setAssignTime(createdAt.plusMinutes(5));
            }
            if (OrderStatus.PROCESSING.equals(status)
                    || OrderStatus.COMPLETED.equals(status)
                    || OrderStatus.CONFIRMED.equals(status)) {
                order.setAcceptTime(createdAt.plusMinutes(35));
            }
            if (OrderStatus.COMPLETED.equals(status) || OrderStatus.CONFIRMED.equals(status)) {
                order.setFinishTime(createdAt.plusHours(2));
            }
            if (OrderStatus.CONFIRMED.equals(status)) {
                order.setConfirmTime(createdAt.plusHours(3));
                order.setRating(BigDecimal.valueOf(4 + random.nextInt(2))); // 4 或 5 分
                order.setRatingComment("维修及时，服务满意");
            }
            if (OrderStatus.CANCELLED.equals(status)) {
                order.setCancelReason("报修人自行解决，撤销报修");
            }
            if (OrderStatus.REJECTED.equals(status)) {
                order.setRejectReason("报修信息不完整，请补充故障照片后重新提交");
            }
            order.setCreatedAt(createdAt);
            repairOrderMapper.insert(order);

            // 伪造与该状态流转一致的操作日志（时间错开）
            writeSeedLogs(order, reporter, admin, repairerId, createdAt);
        }
        log.info("已为报修人 {} 生成 {} 条测试工单（7 种状态全覆盖），并补齐对应 order_log",
                SEED_REPORTER, SEED_TITLES.length);
    }

    /**
     * 按工单状态伪造操作日志，动作链与 design.md §1.2 状态机一致：
     * - 所有工单：SUBMIT（报修人提交）
     * - ASSIGNED 及之后：ASSIGN（管理员派单）
     * - PROCESSING 及之后：ACCEPT（师傅接单）
     * - COMPLETED / CONFIRMED：FINISH（师傅完成）
     * - CONFIRMED：CONFIRM（报修人确认评价）
     * - CANCELLED：CANCEL（报修人取消）
     * - REJECTED：REJECT（管理员驳回）
     * 时间按工单 createdAt 依次递增，保证 el-timeline 按时间正序渲染。
     */
    private void writeSeedLogs(RepairOrder order, User reporter, User admin, Long repairerId, LocalDateTime createdAt) {
        Long orderId = order.getId();
        String status = order.getStatus();
        Long reporterId = reporter != null ? reporter.getId() : order.getReporterId();

        // 提交（报修人）
        addLog(orderId, reporterId, RoleEnum.REPORTER, "SUBMIT", null, OrderStatus.PENDING,
                "提交报修", createdAt);

        if (OrderStatus.CANCELLED.equals(status)) {
            addLog(orderId, reporterId, RoleEnum.REPORTER, "CANCEL", OrderStatus.PENDING,
                    OrderStatus.CANCELLED, "报修人取消：" + order.getCancelReason(), createdAt.plusMinutes(10));
            return;
        }
        if (OrderStatus.REJECTED.equals(status)) {
            addLog(orderId, admin == null ? null : admin.getId(), RoleEnum.ADMIN, "REJECT", OrderStatus.PENDING,
                    OrderStatus.REJECTED, "管理员驳回：" + order.getRejectReason(), createdAt.plusMinutes(10));
            return;
        }

        // 派单（管理员）
        addLog(orderId, admin == null ? null : admin.getId(), RoleEnum.ADMIN, "ASSIGN", OrderStatus.PENDING,
                OrderStatus.ASSIGNED, "派单给师傅", createdAt.plusMinutes(5));

        // 接单（师傅）
        if (OrderStatus.PROCESSING.equals(status)
                || OrderStatus.COMPLETED.equals(status)
                || OrderStatus.CONFIRMED.equals(status)) {
            addLog(orderId, repairerId, RoleEnum.REPAIRER, "ACCEPT", OrderStatus.ASSIGNED,
                    OrderStatus.PROCESSING, "师傅接单", createdAt.plusMinutes(35));
        }

        // 完成（师傅）
        if (OrderStatus.COMPLETED.equals(status) || OrderStatus.CONFIRMED.equals(status)) {
            addLog(orderId, repairerId, RoleEnum.REPAIRER, "FINISH", OrderStatus.PROCESSING,
                    OrderStatus.COMPLETED, "维修完成", createdAt.plusHours(2));
        }

        // 确认评价（报修人）
        if (OrderStatus.CONFIRMED.equals(status)) {
            addLog(orderId, reporterId, RoleEnum.REPORTER, "CONFIRM", OrderStatus.COMPLETED,
                    OrderStatus.CONFIRMED, "报修人确认并评价：" + order.getRating() + " 分", createdAt.plusHours(3));
        }
    }

    /** 插入一条操作日志（时间手动指定，错开以保证时间线顺序） */
    private void addLog(Long orderId, Long operatorId, String operatorRole, String action,
                        String fromStatus, String toStatus, String remark, LocalDateTime time) {
        OrderLog log = new OrderLog();
        log.setOrderId(orderId);
        log.setOperatorId(operatorId);
        log.setOperatorRole(operatorRole);
        log.setAction(action);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setRemark(remark);
        log.setCreatedAt(time);
        orderLogMapper.insert(log);
    }

    /**
     * 兜底补日志：扫描 order_log 表中没有任何记录的工单，按状态机为它们强行补一份
     * 最小操作日志（至少一条 SUBMIT）。用于修复历史残留工单（如早前版本静态插入、
     * 未写 order_log 的工单）在详情页「操作日志为空」的问题。
     * 该逻辑每次启动都会执行，但仅对「无日志」的工单补写，天然幂等：
     * 已补过日志（即存在至少 1 条 order_log）的工单不会重复补。
     */
    private void backfillMissingLogs() {
        List<RepairOrder> orders = repairOrderMapper.selectList(null);
        if (orders == null || orders.isEmpty()) {
            return;
        }

        User reporter = userService.getByUsername(SEED_REPORTER);
        User admin = userService.getByUsername("admin");

        int fixed = 0;
        for (RepairOrder order : orders) {
            Long hasLog = orderLogMapper.selectCount(new LambdaQueryWrapper<OrderLog>()
                    .eq(OrderLog::getOrderId, order.getId()));
            if (hasLog != null && hasLog > 0) {
                continue;
            }

            // 时间基准：优先用工单创建时间，缺失时用当前时间
            LocalDateTime base = order.getCreatedAt() != null ? order.getCreatedAt() : LocalDateTime.now();
            Long reporterId = reporter != null ? reporter.getId() : null;
            Long repairerId = order.getRepairerId();
            String status = order.getStatus() == null ? OrderStatus.PENDING : order.getStatus();

            // 复用 writeSeedLogs 的状态机动作链：传入 reporter（可能为 null，仅用于取 id）
            // 由于该工单不一定是 zhangsan 的，operatorId 使用工单真实 reporter_id 更准确。
            User actualReporter = order.getReporterId() != null ? userService.getById(order.getReporterId()) : null;
            writeSeedLogs(order, actualReporter != null ? actualReporter : reporter, admin, repairerId, base);
            // writeSeedLogs 需要 order.id 已存在；此处 order 来自 selectList，id 一定非空
            log.info("为无日志工单 {} 补齐操作日志（状态：{}）", order.getOrderNo(), status);
            fixed++;
        }
        if (fixed > 0) {
            log.warn("已为 {} 条无日志工单补齐 SUBMIT 等操作日志", fixed);
        }
    }
}
