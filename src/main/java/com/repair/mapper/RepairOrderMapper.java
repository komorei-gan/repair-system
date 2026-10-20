package com.repair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.repair.entity.RepairOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 工单 Mapper。
 */
public interface RepairOrderMapper extends BaseMapper<RepairOrder> {

    /**
     * 按主键查询并加行锁（FOR UPDATE），用于状态流转入口，
     * 防止并发重复操作（如两个管理员同时派单）。
     * 原生 SQL 不经过 MP 逻辑删除插件，需手动带 deleted=0。
     */
    @Select("SELECT * FROM repair_order WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    RepairOrder selectByIdForUpdate(@Param("id") Long id);
}
