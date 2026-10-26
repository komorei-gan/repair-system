package com.repair.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工单表（核心），对应 design.md §2.2。
 */
@Data
@TableName("repair_order")
public class RepairOrder implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务单号（如 RO20261020...） */
    private String orderNo;

    /** 报修人，逻辑关联 user.id */
    private Long reporterId;

    /** 故障分类，逻辑关联 category.id */
    private Long categoryId;

    /** 故障标题 */
    private String title;

    /** 故障描述 */
    private String description;

    /** 图片 URL 的 JSON 数组 */
    private String images;

    /** 报修地址 */
    private String address;

    /** 联系电话 */
    private String contactPhone;

    /** 状态码，见 OrderStatus */
    private String status;

    /** 维修师傅，逻辑关联 user.id */
    private Long repairerId;

    private LocalDateTime assignTime;

    private LocalDateTime acceptTime;

    private LocalDateTime finishTime;

    private LocalDateTime confirmTime;

    /** 驳回/拒单原因 */
    private String rejectReason;

    /** 师傅申请理由（申请改派 / 申请仲裁） */
    private String applyReason;

    /** 师傅申请证据图片 URL 的 JSON 数组（申请改派 / 申请仲裁，选填） */
    private String applyImages;

    /** 取消原因 */
    private String cancelReason;

    /** 维修结果描述 */
    private String repairResult;

    /** 报修人评分（1~5） */
    private BigDecimal rating;

    /** 评价内容 */
    private String ratingComment;

    /** 催单次数（限频用） */
    private Integer remindCount;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除：0 未删除 / 1 已删除 */
    @TableLogic
    private Integer deleted;
}
