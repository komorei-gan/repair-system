package com.repair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.repair.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

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

    /**
     * 按关键字匹配报修人（real_name 或 username 模糊），返回报修人主键集合。
     * 用于管理端工单列表「按报修人搜索」的联表过滤（keyword 不能直接 inSql 拼接，防止注入）。
     */
    @Select("SELECT id FROM user WHERE deleted = 0 AND role = 'REPORTER' "
            + "AND (real_name LIKE CONCAT('%', #{keyword}, '%') OR username LIKE CONCAT('%', #{keyword}, '%'))")
    List<Long> selectReporterIdsByKeyword(@Param("keyword") String keyword);

    /**
     * 按用户名查询（包含已被逻辑删除的记录），用于启动时强制兜底修复种子账号。
     * 原生 SQL 不经过 MP 逻辑删除插件，故可查到 deleted=1 的脏数据。
     */
    @Select("SELECT * FROM user WHERE username = #{username} LIMIT 1")
    User selectByUsernameIgnoreDeleted(@Param("username") String username);

    /**
     * 按主键强制重置种子账号（原生 SQL，绕过逻辑删除过滤）：
     * 同时改写 password / role / status / deleted，保证账号可用。
     */
    @Update("UPDATE user SET password = #{password}, role = #{role}, status = 1, deleted = 0 "
            + "WHERE id = #{id}")
    int forceResetSeedUser(@Param("id") Long id,
                           @Param("password") String password,
                           @Param("role") String role);
}
