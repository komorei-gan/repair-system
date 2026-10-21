package com.repair.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 未读数响应（design.md §4.2 轮询专用，只返回 {"unreadCount": n}）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountVO {

    private Long unreadCount;
}
