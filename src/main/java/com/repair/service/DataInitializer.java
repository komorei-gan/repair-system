package com.repair.service;

import com.repair.common.enums.RoleEnum;
import com.repair.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 数据初始化：首次启动时创建默认管理员账号 admin / admin123。
 * 正式部署后请立即修改密码。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;

    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
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
}
