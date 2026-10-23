package com.repair.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.repair.common.result.PageResult;
import com.repair.dto.RepairerAuditVO;
import com.repair.entity.User;

/**
 * 师傅审核业务（独立成模块，便于后续「独立师傅端 / 信誉分」阶段扩展）。
 */
public interface RepairerAuditService {

    /** 师傅审核分页查询：按审核状态（可选）与关键字（账号/姓名模糊，可选）筛选 */
    PageResult<RepairerAuditVO> pageAudit(Page<User> page, Integer auditStatus, String keyword);

    /** 通过/拒绝师傅（1 通过 / 2 拒绝），并给该师傅发送系统通知 */
    void audit(Long id, Integer auditStatus);
}
