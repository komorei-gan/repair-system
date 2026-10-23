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
 * 用户表（三端角色共用），对应 design.md §2.1。
 */
@Data
@TableName("user")
public class User implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号（唯一） */
    private String username;

    /** BCrypt 加密密码，任何接口不得返回 */
    private String password;

    /** 姓名 */
    private String realName;

    /** 手机号 */
    private String phone;

    /** 角色：REPORTER 报修人 / REPAIRER 师傅 / ADMIN 管理员 */
    private String role;

    /** 1 启用 / 0 禁用 */
    private Integer status;

    /** 审核状态：0 待审核 / 1 已通过 / 2 已拒绝（报修人、管理员默认 1） */
    private Integer auditStatus;

    /** 师傅维修类别，逻辑关联 category.id（仅 REPAIRER 使用） */
    private Long skillCategoryId;

    /** 申请理由（仅师傅注册时填写） */
    private String reason;

    /** 师傅平均评分（仅 REPAIRER 使用） */
    private BigDecimal rating;

    /** 师傅累计完成单量（仅 REPAIRER 使用） */
    private Integer finishCount;

    /** 头像 URL */
    private String avatar;

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
