package com.repair.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 统一分页返回结构（预留，工单/通知列表接口使用）。
 */
@Data
public class PageResult<T> implements Serializable {

    private List<T> records;

    private long total;

    private long pageNum;

    private long pageSize;

    public static <T> PageResult<T> of(List<T> records, long total, long pageNum, long pageSize) {
        PageResult<T> page = new PageResult<>();
        page.setRecords(records);
        page.setTotal(total);
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        return page;
    }

    /** 由 MyBatis-Plus 分页对象直接转换 */
    public static <T> PageResult<T> from(IPage<T> page) {
        return of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }
}
