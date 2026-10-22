package com.repair.service;

import com.repair.common.enums.RoleEnum;
import com.repair.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 联调报修人账号兜底修复器 —— <b>仅限本地开发环境（dev profile）</b>。
 *
 * <p>背景：移动端联调需要稳定的报修人测试账号，但历史测试可能修改过 zhangsan 的密码，
 * 导致 {@code zhangsan / zhangsan123} 登录返回 401。本组件在每次启动时强制校正该账号。
 *
 * <p>行为：
 * <ol>
 *   <li>账号不存在（含被逻辑删除）→ 直接创建；</li>
 *   <li>账号已存在但密码不是 zhangsan123 / 角色不是 REPORTER / 被禁用 / 被逻辑删除
 *       → 强制重置密码为 zhangsan123，并校正 role=REPORTER、status=1、deleted=0。</li>
 * </ol>
 *
 * <p><b>安全约束</b>：类上标注 {@code @Profile("dev")}，生产环境（prod 或未指定 profile）
 * 下本 Bean 根本不会被创建，因此<strong>绝不会</strong>执行任何密码重置逻辑。
 * 生产部署必须以 {@code --spring.profiles.active=prod} 启动。
 *
 * <p>执行顺序：{@code @Order(1)} 先于 {@link DataInitializer}（{@code @Order(2)}）执行，
 * 保证种子工单生成时 zhangsan 已存在且可用。
 */
@Slf4j
@Component
@Profile("dev")
@Order(1)
@RequiredArgsConstructor
public class DevReporterAccountResetter implements CommandLineRunner {

    /** 联调报修人账号 */
    private static final String SEED_REPORTER = "zhangsan";

    /** 联调报修人明文密码（仅 dev 环境使用） */
    private static final String SEED_REPORTER_PASSWORD = "zhangsan123";

    private final UserService userService;

    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        resetReporterAccount();
    }

    /**
     * 强制校正 zhangsan 账号：不存在则创建，密码/角色/状态异常则重置。
     * 幂等：状态完全正常时不产生任何写操作。
     */
    private void resetReporterAccount() {
        User existing = userService.getByUsernameIgnoreDeleted(SEED_REPORTER);

        // 1. 不存在（含已逻辑删除）→ 创建
        if (existing == null) {
            User reporter = new User();
            reporter.setUsername(SEED_REPORTER);
            reporter.setPassword(passwordEncoder.encode(SEED_REPORTER_PASSWORD));
            reporter.setRealName("张三");
            reporter.setRole(RoleEnum.REPORTER);
            reporter.setStatus(1);
            reporter.setFinishCount(0);
            userService.save(reporter);
            log.warn("[dev] 已创建联调报修人账号 {} / {}（仅本地开发环境）",
                    SEED_REPORTER, SEED_REPORTER_PASSWORD);
            return;
        }

        // 2. 已存在 → 逐项校验
        boolean passwordMatched = passwordEncoder.matches(SEED_REPORTER_PASSWORD, existing.getPassword());
        boolean roleMatched = RoleEnum.REPORTER.equals(existing.getRole());
        boolean enabled = existing.getStatus() != null && existing.getStatus() == 1;
        boolean notDeleted = existing.getDeleted() == null || existing.getDeleted() == 0;

        if (passwordMatched && roleMatched && enabled && notDeleted) {
            log.info("[dev] 报修人账号 {} 状态正常，跳过兜底重置", SEED_REPORTER);
            return;
        }

        // 3. 任一异常 → 强制重置（密码 / 角色 / 启用状态 / 逻辑删除标记一并修正）
        userService.forceResetSeedUser(existing.getId(), SEED_REPORTER_PASSWORD, RoleEnum.REPORTER);
        log.warn("[dev] 已强制重置报修人账号 {} / {}（原状态：密码匹配={}、角色={}、启用={}、未删除={}）",
                SEED_REPORTER, SEED_REPORTER_PASSWORD, passwordMatched, existing.getRole(), enabled, notDeleted);
    }
}
