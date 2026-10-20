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
 * 故障分类表，对应 design.md §2.5。
 */
@Data
@TableName("category")
public class Category implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类名（如 水电/门窗/网络） */
    private String name;

    /** 排序值 */
    private Integer sort;

    /** 1 启用 / 0 停用 */
    private Integer status;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
