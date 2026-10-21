package com.repair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.repair.entity.Notification;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 通知 Mapper。
 */
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * 未读数统计（轮询专用，轻量）。
     * 条件 user_id + is_read 命中 design.md §2.3 中的 idx_user_read(user_id, is_read, created_at) 索引，
     * 单表 COUNT 查询，无联表、无缓存，单机规模足够支撑 30s 轮询。
     */
    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId} AND is_read = 0")
    long countUnread(@Param("userId") Long userId);
}
