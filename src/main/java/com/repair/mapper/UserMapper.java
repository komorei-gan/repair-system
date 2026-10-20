package com.repair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.repair.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper（启动类 @MapperScan 已扫描 com.repair.mapper，无需再标 @Mapper）。
 */
public interface UserMapper extends BaseMapper<User> {

    /**
     * 按主键查询并加行锁（FOR UPDATE），用于工单确认时更新师傅聚合数据
     * （rating / finish_count），防止并发确认导致聚合数据丢失更新。
     * 原生 SQL 不经过 MP 逻辑删除插件，需手动带 deleted=0。
     */
    @Select("SELECT * FROM user WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    User selectByIdForUpdate(@Param("id") Long id);
}
