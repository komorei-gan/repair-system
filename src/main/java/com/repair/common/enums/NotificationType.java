package com.repair.common.enums;

/**
 * 通知类型常量（design.md §2.3：ORDER 工单通知 / SYSTEM 系统通知）。
 */
public final class NotificationType {

    /** 工单通知 */
    public static final String ORDER = "ORDER";

    /** 系统通知 */
    public static final String SYSTEM = "SYSTEM";

    private NotificationType() {
    }
}
