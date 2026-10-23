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
 * 工单操作日志表（状态审计），对应 design.md §2.4。
 */
@Data
@TableName("order_log")
public class OrderLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 逻辑关联 repair_order.id */
    private Long orderId;

    /** 操作人，逻辑关联 user.id */
    private Long operatorId;

    /** 操作人角色 */
    private String operatorRole;

    /** 动作：SUBMIT/ASSIGN/ACCEPT/REJECT/FINISH/CONFIRM/CANCEL/REMIND/UPDATE */
    private String action;

    /** 变更前状态 */
    private String fromStatus;

    /** 变更后状态 */
    private String toStatus;

    /** 备注（原因、结果摘要） */
    private String remark;

    /** 操作时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
