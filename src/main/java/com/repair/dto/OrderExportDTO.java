package com.repair.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 工单导出 DTO（管理端 Excel 导出，EasyExcel 列定义）。
 * 状态与创建时间在 Service 层已完成中文映射与格式化，此处仅做列头声明。
 */
@Data
public class OrderExportDTO {

    /** 工单号 */
    @ExcelProperty("工单号")
    private String orderNo;

    /** 故障标题 */
    @ExcelProperty("故障标题")
    private String title;

    /** 故障分类名 */
    @ExcelProperty("分类")
    private String categoryName;

    /** 报修人姓名（real_name 优先） */
    @ExcelProperty("报修人")
    private String reporterName;

    /** 维修师傅姓名（可能为空，未派单时导出空单元格） */
    @ExcelProperty("维修师傅")
    private String repairerName;

    /** 状态中文名（已映射，如「维修中」） */
    @ExcelProperty("状态")
    private String status;

    /** 创建时间（yyyy-MM-dd HH:mm:ss） */
    @ExcelProperty("创建时间")
    private String createdAt;
}
