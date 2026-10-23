package com.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.repair.common.enums.AuditStatus;
import com.repair.common.enums.NotificationType;
import com.repair.common.enums.RoleEnum;
import com.repair.common.exception.BusinessException;
import com.repair.common.result.ErrorCode;
import com.repair.common.result.PageResult;
import com.repair.dto.RepairerAuditVO;
import com.repair.entity.User;
import com.repair.mapper.UserMapper;
import com.repair.service.NotifyService;
import com.repair.service.RepairerAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 师傅审核业务实现。
 */
@Service
@RequiredArgsConstructor
public class RepairerAuditServiceImpl implements RepairerAuditService {

    private final UserMapper userMapper;

    private final NotifyService notifyService;

    @Override
    public PageResult<RepairerAuditVO> pageAudit(Page<User> page, Integer auditStatus, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getRole, RoleEnum.REPAIRER)
                .eq(auditStatus != null, User::getAuditStatus, auditStatus)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(User::getRealName, keyword)
                        .or()
                        .like(User::getUsername, keyword))
                .orderByDesc(User::getId);

        Page<User> result = userMapper.selectPage(page, wrapper);
        return PageResult.from(result.convert(RepairerAuditVO::from));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, Integer auditStatus) {
        // 管理端仅允许「通过 / 拒绝」，不允许反向置回待审核
        if (auditStatus == null || (auditStatus != AuditStatus.APPROVED && auditStatus != AuditStatus.REJECTED)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "审核结果非法，仅支持通过或拒绝");
        }

        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (!RoleEnum.REPAIRER.equals(user.getRole())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该用户不是维修师傅");
        }
        // 已是目标状态则幂等返回，避免重复更新与重复通知
        if (auditStatus.equals(user.getAuditStatus())) {
            return;
        }

        userMapper.update(null, new UpdateWrapper<User>()
                .eq("id", id)
                .set("audit_status", auditStatus));

        // 审核状态变更写入系统通知（AGENTS.md：状态变更必须写 notification）
        boolean approved = auditStatus == AuditStatus.APPROVED;
        String title = approved ? "师傅审核通过" : "师傅审核未通过";
        String content = approved
                ? "您的维修师傅申请已通过审核，欢迎加入维修团队。"
                : "很抱歉，您的维修师傅申请未通过审核，如有疑问请联系管理员。";
        notifyService.notify(id, NotificationType.SYSTEM, title, content, null);
    }
}
