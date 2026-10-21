package com.repair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.repair.common.enums.OrderStatus;
import com.repair.common.enums.RoleEnum;
import com.repair.common.util.OrderNoUtil;
import com.repair.entity.Category;
import com.repair.entity.RepairOrder;
import com.repair.entity.User;
import com.repair.mapper.CategoryMapper;
import com.repair.mapper.RepairOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
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
 * 4. 报修人 zhangsan 名下无工单时，生成 10 条测试工单，
 *    状态均匀覆盖 PENDING / ASSIGNED / PROCESSING / COMPLETED / CONFIRMED（各 2 条）。
 * 正式部署后请立即修改密码。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String SEED_REPORTER = "zhangsan";

    /** 测试工单标题池（仅用于种子数据，与真实业务无关） */
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
    };

    private final UserService userService;

    private final CategoryMapper categoryMapper;

    private final RepairOrderMapper repairOrderMapper;

    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initAdmin();
        initCategories();
        initRepairers();
        initSeedOrders();
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
     * 工单种子数据：报修人 zhangsan 名下无工单时生成 10 条测试工单，
     * 状态均匀覆盖 PENDING / ASSIGNED / PROCESSING / COMPLETED / CONFIRMED（各 2 条）。
     * 以「zhangsan 名下工单数」而非「全表」作为幂等判断，避免表里已有其他工单时跳过种子生成。
     * 说明：种子数据为静态插入，绕过 Service 状态机，不写 order_log / notification，
     * 避免在初始化阶段向管理员与师傅推送无关测试通知。
     */
    private void initSeedOrders() {
        User reporter = userService.getByUsername(SEED_REPORTER);
        if (reporter == null) {
            log.warn("未找到报修人账号 {}，跳过测试工单生成（请先通过注册接口创建该账号）", SEED_REPORTER);
            return;
        }

        long existing = repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getReporterId, reporter.getId()));
        if (existing > 0) {
            log.info("报修人 {} 名下已有 {} 条工单，跳过测试工单生成", SEED_REPORTER, existing);
            return;
        }

        List<Category> categories = categoryMapper.selectList(null);
        if (categories == null || categories.isEmpty()) {
            log.warn("故障分类为空，跳过测试工单生成");
            return;
        }

        User repairer1 = userService.getByUsername("master");
        User repairer2 = userService.getByUsername("master2");
        List<Long> repairerIds = new ArrayList<>();
        if (repairer1 != null) {
            repairerIds.add(repairer1.getId());
        }
        if (repairer2 != null) {
            repairerIds.add(repairer2.getId());
        }

        // 5 种状态各 2 条，共 10 条，与 SEED_TITLES 一一对应
        String[] statuses = {
                OrderStatus.PENDING, OrderStatus.PENDING,
                OrderStatus.ASSIGNED, OrderStatus.ASSIGNED,
                OrderStatus.PROCESSING, OrderStatus.PROCESSING,
                OrderStatus.COMPLETED, OrderStatus.COMPLETED,
                OrderStatus.CONFIRMED, OrderStatus.CONFIRMED,
        };

        ThreadLocalRandom random = ThreadLocalRandom.current();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < SEED_TITLES.length; i++) {
            String status = statuses[i];
            Category category = categories.get(random.nextInt(categories.size()));

            // PENDING 无师傅；其余状态随机指派师傅（师傅不存在则留空）
            boolean hasRepairer = !OrderStatus.PENDING.equals(status);
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
            order.setCreatedAt(createdAt);
            repairOrderMapper.insert(order);
        }
        log.info("已为报修人 {} 生成 {} 条测试工单，状态均匀覆盖 PENDING / ASSIGNED / PROCESSING / COMPLETED / CONFIRMED",
                SEED_REPORTER, SEED_TITLES.length);
    }
}
