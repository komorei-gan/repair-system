package com.repair.service;

import com.repair.common.enums.RoleEnum;
import com.repair.entity.Category;
import com.repair.entity.User;
import com.repair.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 数据初始化（幂等，仅首次启动创建）：
 * 1. 默认管理员账号 admin / admin123；
 * 2. 默认故障分类：水电 / 门窗 / 网络 / 家具 / 其他；
 * 3. 两个联调用维修师傅：master / master123、master2 / master123。
 * 正式部署后请立即修改密码。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;

    private final CategoryMapper categoryMapper;

    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initAdmin();
        initCategories();
        initRepairers();
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
}
