package com.repair.common.enums;

/**
 * 账号审核状态常量（design.md §2.1 user.audit_status）。
 * 与 {@link com.repair.entity.User#status}（启用/禁用）是两个正交维度：
 * 本常量只描述「师傅注册后的审核流转」，报修人与管理员默认 APPROVED。
 */
public final class AuditStatus {

    /** 待审核（仅师傅注册后进入该状态） */
    public static final int PENDING = 0;

    /** 已通过 */
    public static final int APPROVED = 1;

    /** 已拒绝 */
    public static final int REJECTED = 2;

    private AuditStatus() {
    }

    /** 是否为合法的审核状态值 */
    public static boolean isValid(Integer auditStatus) {
        return auditStatus != null
                && (auditStatus == PENDING || auditStatus == APPROVED || auditStatus == REJECTED);
    }
}
