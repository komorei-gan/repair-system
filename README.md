# 报修工单系统 — 后端（repair-system）

第一阶段：基础工程 + 用户认证模块；第二阶段：工单核心模块（完整状态机）；第三阶段：通知读取模块。

技术栈：Spring Boot 3.3.5 · JDK 17 · MySQL 8 · MyBatis-Plus 3.5.7 · Spring Security + JWT (jjwt 0.12.6) · Lombok

## 快速启动

1. 环境要求：JDK 17、Maven 3.9+、MySQL 8（本地 3306）。
2. 数据库账号默认 `root / 123456`，可用环境变量覆盖（PowerShell 示例）：

   ```powershell
   $env:DB_USERNAME = "root"
   $env:DB_PASSWORD = "你的密码"
   ```

3. 启动：

   ```powershell
   mvn spring-boot:run
   ```

   - 首次启动自动创建数据库 `repair_system` 及全部 5 张表（脚本见 `src/main/resources/sql/schema.sql`，幂等可重复执行）；
   - 自动创建种子数据（正式部署请立即改密）：
     - 管理员 **admin / admin123**；
     - 联调师傅 **master / master123**（王师傅）、**master2 / master123**（李师傅）；
     - 故障分类：水电、门窗、网络、家具、其他。

4. 验证：访问 `http://localhost:8080/api/auth/me` 应返回 401（未登录）。

## 接口清单

统一返回 `Result<T>`：`{"code":200,"msg":"...","data":...}`，`code` 与 HTTP 状态码一致（200 成功 / 400 参数错误 / 401 未登录 / 403 无权限 / 404 / 500）。分页参数 `pageNum/pageSize`。⭐ = 状态变更接口（同一事务写入 `notification`）。

### 认证（design.md §3.1，三端共用）

| Method | URL | 说明 |
| --- | --- | --- |
| POST | /api/auth/login | 登录，返回 `token` + 用户信息（含角色） |
| POST | /api/auth/register | 报修人注册（角色固定 REPORTER） |
| GET | /api/auth/me | 当前登录用户信息，请求头 `Authorization: Bearer <token>` |

### 报修端（design.md §3.2，REPORTER）

| Method | URL | 说明 |
| --- | --- | --- |
| ⭐ POST | /api/orders | 提交报修工单 → 通知管理员 |
| GET | /api/orders/my | 我的报修列表（分页，`?status=` 筛选） |
| GET | /api/orders/{id} | 工单详情（含操作日志；三端可访问，归属校验在服务端） |
| ⭐ PUT | /api/orders/{id}/cancel | 取消工单（仅 PENDING，原因必填）→ 通知管理员 |
| ⭐ POST | /api/orders/{id}/confirm | 确认并评价（仅 COMPLETED，评分 1~5）→ 通知师傅 |
| ⭐ POST | /api/orders/{id}/remind | 催单（仅 PROCESSING，限频：≥3 次或 30 分钟内已催）→ 通知师傅 |
| GET | /api/categories | 故障分类列表（登录即可） |

### 维修端（design.md §3.3，REPAIRER）

| Method | URL | 说明 |
| --- | --- | --- |
| GET | /api/repairer/orders | 我的维修任务（分页，`?status=` 筛选） |
| ⭐ PUT | /api/repairer/orders/{id}/accept | 接单（仅 ASSIGNED、仅被指派师傅）→ 通知报修人 |
| ⭐ PUT | /api/repairer/orders/{id}/reject | 拒单（仅 ASSIGNED，原因必填）→ 通知管理员 |
| ⭐ PUT | /api/repairer/orders/{id}/complete | 提交完成（仅 PROCESSING，结果必填）→ 通知报修人 |
| GET | /api/repairer/stats | 我的统计（完成数、平均评分、维修中/待接单数） |

### 管理端（design.md §3.4，ADMIN）

| Method | URL | 说明 |
| --- | --- | --- |
| GET | /api/admin/orders | 工单分页（`?status=&categoryId=&keyword=&startTime=&endTime=`） |
| ⭐ PUT | /api/admin/orders/{id}/assign | 派单（仅 PENDING，师傅必填）→ 通知师傅 |
| ⭐ PUT | /api/admin/orders/{id}/reassign | 改派（仅 ASSIGNED/PROCESSING，新师傅≠现任）→ 通知新旧师傅 |
| ⭐ PUT | /api/admin/orders/{id}/reject | 驳回（仅 PENDING，原因必填）→ 通知报修人 |
| ⭐ PUT | /api/admin/orders/{id}/cancel | 管理员取消（仅 PENDING/ASSIGNED，原因必填）→ 通知报修人（有师傅则一并） |

## 通知接口（design.md §3.5，三端通用，登录即可）

| Method | URL | 说明 |
| --- | --- | --- |
| GET | /api/notifications | 我的通知列表（分页，按时间倒序，`?isRead=0/1` 筛选） |
| GET | /api/notifications/unread-count | 未读数（轮询专用，`SELECT COUNT(*)` 走 `idx_user_read` 索引） |
| PUT | /api/notifications/{id}/read | 标记单条已读（幂等；仅本人通知） |
| PUT | /api/notifications/read-all | 全部标记已读 |

> 所有接口的 userId 均从 SecurityContext 获取，客户端无法指定；轮询建议 30s 一次（design.md §4.2）。

## 工单状态机（design.md §1）

```
PENDING → ASSIGNED（派单）/ REJECTED（驳回）/ CANCELLED（取消）
ASSIGNED → PROCESSING（接单）/ PENDING（拒单）/ ASSIGNED（改派）/ CANCELLED（管理员取消）
PROCESSING → COMPLETED（完成）/ ASSIGNED（改派）
COMPLETED → CONFIRMED（确认评价）
CONFIRMED / CANCELLED / REJECTED 为终态
```

状态流转全部收敛在 `OrderServiceImpl`，非法跳转返回 400「当前状态不允许该操作」；每次状态变更在同一事务内更新工单、写 `order_log`、写 `notification`（强一致）。

## 目录结构

```
src/main/java/com/repair
├── common      # 统一返回 / 错误码 / 分页 / 异常 / 枚举（含工单状态机）/ 工具
├── config      # MyBatis-Plus / Security / CORS / JWT 属性
├── security    # JWT 工具 / 认证过滤器 / 当前用户
├── entity      # 数据库实体（user/repair_order/notification/order_log/category）
├── mapper      # MyBatis-Plus Mapper（工单/用户含 FOR UPDATE 行锁查询）
├── dto         # 请求 / 响应对象
├── service     # 业务逻辑（状态流转 + 同事务通知 + 初始化）
└── controller  # 仅接收参数与返回结果（@PreAuthorize 角色控制）
```

## 生产注意事项

- `JWT_SECRET`、`DB_USERNAME`、`DB_PASSWORD` 必须用环境变量覆盖默认值；
- `app.cors.allowed-origin-patterns` 上线收紧为具体前端域名；
- 开发期 SQL 日志（StdOutImpl）上线改为 `org.apache.ibatis.logging.nologging.NoLoggingImpl`；
- 种子账号（admin / master / master2）上线前必须删除或改密。
