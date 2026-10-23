-- =============================================================
-- 报修工单系统 建表脚本（对应 design.md §2 数据库表设计）
-- MySQL 8 / utf8mb4；表间仅逻辑关联，不建物理外键
-- 由 spring.sql.init 在应用启动时自动执行（幂等，可重复运行）
-- =============================================================

CREATE TABLE IF NOT EXISTS `user` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`          VARCHAR(50)  NOT NULL                COMMENT '登录账号',
    `password`          VARCHAR(100) NOT NULL                COMMENT 'BCrypt 加密密码',
    `real_name`         VARCHAR(50)           DEFAULT NULL   COMMENT '姓名',
    `phone`             VARCHAR(20)           DEFAULT NULL   COMMENT '手机号',
    `role`              VARCHAR(20)  NOT NULL                COMMENT '角色：REPORTER 报修人 / REPAIRER 师傅 / ADMIN 管理员',
    `status`            TINYINT      NOT NULL DEFAULT 1      COMMENT '1 启用 / 0 禁用',
    `audit_status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '审核状态：0 待审核 / 1 已通过 / 2 已拒绝',
    `skill_category_id` BIGINT                DEFAULT NULL   COMMENT '师傅维修类别，逻辑关联 category.id（仅 REPAIRER）',
    `reason`            VARCHAR(500)          DEFAULT NULL   COMMENT '申请理由（仅师傅注册时填写）',
    `reset_approved`    TINYINT      NOT NULL DEFAULT 0      COMMENT '密码重置流转：0 未申请 / 1 已申请待批准 / 2 已批准',
    `rating`            DECIMAL(2,1)          DEFAULT NULL   COMMENT '师傅平均评分（仅 REPAIRER）',
    `finish_count`      INT          NOT NULL DEFAULT 0      COMMENT '师傅累计完成单量（仅 REPAIRER）',
    `avatar`            VARCHAR(255)          DEFAULT NULL   COMMENT '头像 URL',
    `created_at`        DATETIME              DEFAULT NULL   COMMENT '创建时间',
    `updated_at`        DATETIME              DEFAULT NULL   COMMENT '更新时间',
    `deleted`           TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0 未删除 / 1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表（三端角色共用）';

CREATE TABLE IF NOT EXISTS `repair_order` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_no`       VARCHAR(32)   NOT NULL                COMMENT '业务单号（如 RO20261020...）',
    `reporter_id`    BIGINT        NOT NULL                COMMENT '报修人，逻辑关联 user.id',
    `category_id`    BIGINT                 DEFAULT NULL   COMMENT '故障分类，逻辑关联 category.id',
    `title`          VARCHAR(100)  NOT NULL                COMMENT '故障标题',
    `description`    VARCHAR(1000)          DEFAULT NULL   COMMENT '故障描述',
    `images`         VARCHAR(1000)          DEFAULT NULL   COMMENT '图片 URL 的 JSON 数组',
    `address`        VARCHAR(200)           DEFAULT NULL   COMMENT '报修地址',
    `contact_phone`  VARCHAR(20)            DEFAULT NULL   COMMENT '联系电话',
    `status`         VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '状态码，见 design.md §1.1',
    `repairer_id`    BIGINT                 DEFAULT NULL   COMMENT '维修师傅，逻辑关联 user.id',
    `assign_time`    DATETIME               DEFAULT NULL   COMMENT '派单时间',
    `accept_time`    DATETIME               DEFAULT NULL   COMMENT '接单时间',
    `finish_time`    DATETIME               DEFAULT NULL   COMMENT '完成时间',
    `confirm_time`   DATETIME               DEFAULT NULL   COMMENT '确认时间',
    `reject_reason`  VARCHAR(200)           DEFAULT NULL   COMMENT '驳回/拒单原因',
    `cancel_reason`  VARCHAR(200)           DEFAULT NULL   COMMENT '取消原因',
    `repair_result`  VARCHAR(500)           DEFAULT NULL   COMMENT '维修结果描述',
    `rating`         DECIMAL(2,1)           DEFAULT NULL   COMMENT '报修人评分（1~5）',
    `rating_comment` VARCHAR(200)           DEFAULT NULL   COMMENT '评价内容',
    `remind_count`   INT          NOT NULL DEFAULT 0      COMMENT '催单次数（限频用）',
    `created_at`     DATETIME               DEFAULT NULL   COMMENT '创建时间',
    `updated_at`     DATETIME               DEFAULT NULL   COMMENT '更新时间',
    `deleted`        TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0 未删除 / 1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_reporter` (`reporter_id`),
    KEY `idx_repairer` (`repairer_id`),
    KEY `idx_status_created` (`status`, `created_at`),
    KEY `idx_category` (`category_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '工单表（核心）';

CREATE TABLE IF NOT EXISTS `notification` (
    `id`         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`    BIGINT        NOT NULL                COMMENT '接收人，逻辑关联 user.id',
    `type`       VARCHAR(20)   NOT NULL                COMMENT '通知类型：ORDER 工单通知 / SYSTEM 系统通知',
    `title`      VARCHAR(100)  NOT NULL                COMMENT '标题（冗余，展示不联表）',
    `content`    VARCHAR(500)           DEFAULT NULL   COMMENT '内容（冗余，展示不联表）',
    `order_id`   BIGINT                 DEFAULT NULL   COMMENT '关联工单，逻辑关联 repair_order.id，点击可跳转',
    `is_read`    TINYINT       NOT NULL DEFAULT 0      COMMENT '0 未读 / 1 已读',
    `read_time`  DATETIME               DEFAULT NULL   COMMENT '阅读时间',
    `created_at` DATETIME               DEFAULT NULL   COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_read` (`user_id`, `is_read`, `created_at`),
    KEY `idx_order` (`order_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '通知表';

CREATE TABLE IF NOT EXISTS `order_log` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`      BIGINT       NOT NULL                COMMENT '逻辑关联 repair_order.id',
    `operator_id`   BIGINT                DEFAULT NULL   COMMENT '操作人，逻辑关联 user.id',
    `operator_role` VARCHAR(20)           DEFAULT NULL   COMMENT '操作人角色',
    `action`        VARCHAR(30)  NOT NULL                COMMENT '动作：SUBMIT/ASSIGN/ACCEPT/REJECT/FINISH/CONFIRM/CANCEL/REMIND',
    `from_status`   VARCHAR(20)           DEFAULT NULL   COMMENT '变更前状态',
    `to_status`     VARCHAR(20)           DEFAULT NULL   COMMENT '变更后状态',
    `remark`        VARCHAR(200)          DEFAULT NULL   COMMENT '备注（原因、结果摘要）',
    `created_at`    DATETIME              DEFAULT NULL   COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '工单操作日志表（状态审计）';

CREATE TABLE IF NOT EXISTS `category` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`       VARCHAR(50) NOT NULL                COMMENT '分类名（如 水电/门窗/网络）',
    `sort`       INT         NOT NULL DEFAULT 0      COMMENT '排序值',
    `status`     TINYINT     NOT NULL DEFAULT 1      COMMENT '1 启用 / 0 停用',
    `created_at` DATETIME             DEFAULT NULL   COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '故障分类表';
