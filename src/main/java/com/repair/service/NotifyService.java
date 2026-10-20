package com.repair.service;

import java.util.Collection;

/**
 * 通知写入服务（design.md §4.1）。
 * 与状态流转同事务调用：状态变更方法加 @Transactional，
 * 先更新工单，再调用本服务插入通知，保证「状态变了，通知必到」。
 * 本服务方法自身不加 @Transactional、不使用 @Async，通过代理调用加入外层事务。
 */
public interface NotifyService {

    /**
     * 给单个接收人插入一条通知。
     */
    void notify(Long userId, String type, String title, String content, Long orderId);

    /**
     * 给多个接收人各插入一条通知（批量接收，如提交报修通知所有管理员）。
     */
    void notifyUsers(Collection<Long> userIds, String type, String title, String content, Long orderId);
}
