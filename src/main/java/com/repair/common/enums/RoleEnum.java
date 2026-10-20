package com.repair.common.enums;

/**
 * 角色常量（design.md：角色用 varchar 存储，不使用魔法数字）。
 */
public final class RoleEnum {

    /** 报修人 */
    public static final String REPORTER = "REPORTER";

    /** 维修师傅 */
    public static final String REPAIRER = "REPAIRER";

    /** 管理员 */
    public static final String ADMIN = "ADMIN";

    private RoleEnum() {
    }

    public static boolean isValid(String role) {
        return REPORTER.equals(role) || REPAIRER.equals(role) || ADMIN.equals(role);
    }
}
