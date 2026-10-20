package com.repair.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 工单分页查询参数（报修人 / 管理员共用；师傅侧复用，按需扩展）。
 * 统一分页参数 pageNum/pageSize 由 MyBatis-Plus Page 接收。
 */
@Data
public class OrderQuery {

    /** 按状态筛选，见 OrderStatus */
    private String status;

    /** 按故障分类筛选（管理员） */
    private Long categoryId;

    /** 关键字：匹配工单标题或单号（管理员） */
    private String keyword;

    /** 创建时间起（管理员，ISO 格式如 2026-10-01T00:00:00） */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    /** 创建时间止（管理员） */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;
}
