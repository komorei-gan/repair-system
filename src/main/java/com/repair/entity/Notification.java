package com.repair.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知表，对应 design.md §2.3。
 * title/content 冗余存储，列表展示不联表。
 */
@Data
@TableName("notification")
public class Notification implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收人，逻辑关联 user.id */
    private Long userId;

    /** 通知类型：ORDER 工单通知 / SYSTEM 系统通知 */
    private String type;

    /** 标题（冗余，展示不联表） */
    private String title;

    /** 内容（冗余，展示不联表） */
    private String content;

    /** 关联工单，逻辑关联 repair_order.id，点击可跳转 */
    private Long orderId;

    /** 0 未读 / 1 已读 */
    private Integer isRead;

    private LocalDateTime readTime;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
