package com.repair.dto;

import com.repair.entity.OrderLog;
import com.repair.entity.RepairOrder;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单详情视图（含操作日志，design.md §3.2 GET /api/orders/{id}）。
 */
@Data
@Builder
public class OrderDetailVO {

    private Long id;

    private String orderNo;

    private Long reporterId;

    private String reporterName;

    private Long categoryId;

    private String categoryName;

    private String title;

    private String description;

    private String images;

    private String address;

    private String contactPhone;

    private String status;

    private Long repairerId;

    private String repairerName;

    private String repairerPhone;

    /** 师傅申请理由（申请改派 / 申请仲裁） */
    private String applyReason;

    /** 师傅申请证据图片 URL 的 JSON 数组 */
    private String applyImages;

    private LocalDateTime assignTime;

    private LocalDateTime acceptTime;

    private LocalDateTime finishTime;

    private LocalDateTime confirmTime;

    private String rejectReason;

    private String cancelReason;

    private String repairResult;

    private java.math.BigDecimal rating;

    private String ratingComment;

    private Integer remindCount;

    private LocalDateTime createdAt;

    /** 操作日志（时间正序） */
    private List<OrderLogVO> logs;

    public static OrderDetailVO from(RepairOrder order, String reporterName, String categoryName,
                                     String repairerName, String repairerPhone, List<OrderLogVO> logs) {
        return OrderDetailVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .reporterId(order.getReporterId())
                .reporterName(reporterName)
                .categoryId(order.getCategoryId())
                .categoryName(categoryName)
                .title(order.getTitle())
                .description(order.getDescription())
                .images(order.getImages())
                .address(order.getAddress())
                .contactPhone(order.getContactPhone())
                .status(order.getStatus())
                .repairerId(order.getRepairerId())
                .repairerName(repairerName)
                .repairerPhone(repairerPhone)
                .applyReason(order.getApplyReason())
                .applyImages(order.getApplyImages())
                .assignTime(order.getAssignTime())
                .acceptTime(order.getAcceptTime())
                .finishTime(order.getFinishTime())
                .confirmTime(order.getConfirmTime())
                .rejectReason(order.getRejectReason())
                .cancelReason(order.getCancelReason())
                .repairResult(order.getRepairResult())
                .rating(order.getRating())
                .ratingComment(order.getRatingComment())
                .remindCount(order.getRemindCount())
                .createdAt(order.getCreatedAt())
                .logs(logs)
                .build();
    }

    /**
     * 操作日志视图（含操作人姓名）。
     */
    @Data
    @Builder
    public static class OrderLogVO {

        private Long id;

        private Long operatorId;

        private String operatorName;

        private String operatorRole;

        private String action;

        private String fromStatus;

        private String toStatus;

        private String remark;

        private LocalDateTime createdAt;

        public static OrderLogVO from(OrderLog log, String operatorName) {
            return OrderLogVO.builder()
                    .id(log.getId())
                    .operatorId(log.getOperatorId())
                    .operatorName(operatorName)
                    .operatorRole(log.getOperatorRole())
                    .action(log.getAction())
                    .fromStatus(log.getFromStatus())
                    .toStatus(log.getToStatus())
                    .remark(log.getRemark())
                    .createdAt(log.getCreatedAt())
                    .build();
        }
    }
}
