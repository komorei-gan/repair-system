package com.repair.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 时间格式配置。
 *
 * <p><b>为什么需要这个类：</b>
 * {@code spring.jackson.date-format} 只对 {@link java.util.Date} 生效，
 * 对 {@link LocalDateTime} 无效——后者由 JavaTimeModule 按 ISO-8601 序列化，
 * 输出形如 {@code 2026-10-20T22:16:12}（带 T）。本项目 DTO 全部使用 LocalDateTime，
 * 因此在 Java 侧显式注册序列化器，统一输出 {@code yyyy-MM-dd HH:mm:ss}，
 * 与前端展示和 {@code application.yml} 中的 date-format 保持一致。
 *
 * <p>注意：只格式化「不带时区」的 LocalDateTime（数据库 DATETIME 语义），
 * 不做时区换算，避免与 {@code serverTimezone=Asia/Shanghai} 产生双重偏移。
 */
@Configuration
public class JacksonConfig {

    /** 与 application.yml 的 spring.jackson.date-format 保持一致 */
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateTimeCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule();
            // 出参：LocalDateTime -> "yyyy-MM-dd HH:mm:ss"
            module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME_FORMATTER));
            // 入参：按 "yyyy-MM-dd HH:mm:ss" 解析
            module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME_FORMATTER));
            // 用 modulesToInstall 追加而非 modules 覆盖：
            // 后者会顶掉 Spring Boot 自动注册的 JavaTimeModule，
            // 导致 LocalDate / LocalTime 等其他 java.time 类型失去默认支持。
            builder.modulesToInstall(module);
        };
    }
}
