package com.repair.dto;

import com.repair.entity.RepairOrder;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工单列表视图（列表页不返回大字段，如 description/images）。
 */
@Data
@Builder
public class OrderVO {

    private Long id;

    private String orderNo;

    private Long reporterId;

    /** 报修人姓名（冗余，便于师傅/管理员列表展示） */
    private String reporterName;

    private Long categoryId;

    /** 分类名（冗余，便于列表展示） */
    private String categoryName;

    private String title;

    private String address;

    private String contactPhone;

    private String status;

    private Long repairerId;

    /** 师傅姓名（冗余，便于报修人列表展示） */
    private String repairerName;

    private LocalDateTime assignTime;

    private LocalDateTime acceptTime;

    private LocalDateTime finishTime;

    private LocalDateTime confirmTime;

    private BigDecimal rating;

    private Integer remindCount;

    private LocalDateTime createdAt;

    public static OrderVO from(RepairOrder order, String reporterName, String categoryName, String repairerName) {
        return OrderVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .reporterId(order.getReporterId())
                .reporterName(reporterName)
                .categoryId(order.getCategoryId())
                .categoryName(categoryName)
                .title(order.getTitle())
                .address(order.getAddress())
                .contactPhone(order.getContactPhone())
                .status(order.getStatus())
                .repairerId(order.getRepairerId())
                .repairerName(repairerName)
                .assignTime(order.getAssignTime())
                .acceptTime(order.getAcceptTime())
                .finishTime(order.getFinishTime())
                .confirmTime(order.getConfirmTime())
                .rating(order.getRating())
                .remindCount(order.getRemindCount())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
