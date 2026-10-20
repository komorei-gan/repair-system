# 报修工单系统 — 后端（repair-system）

第一阶段：基础工程 + 用户认证模块。

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
   - 自动创建默认管理员账号 **admin / admin123**（正式部署请立即改密）。

4. 验证：访问 `http://localhost:8080/api/auth/me` 应返回 401（未登录）。

## 认证接口（design.md §3.1，三端共用）

| Method | URL | 说明 |
| --- | --- | --- |
| POST | /api/auth/login | 登录，返回 `token` + 用户信息（含角色） |
| POST | /api/auth/register | 报修人注册（角色固定 REPORTER） |
| GET | /api/auth/me | 当前登录用户信息，请求头 `Authorization: Bearer <token>` |

统一返回 `Result<T>`：`{"code":200,"msg":"...","data":...}`，`code` 与 HTTP 状态码一致（200 成功 / 400 参数错误 / 401 未登录 / 403 无权限 / 404 / 500）。

curl 示例：

```bash
# 注册
curl -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d "{\"username\":\"zhangsan\",\"password\":\"123456\",\"realName\":\"张三\",\"phone\":\"13800000000\"}"

# 登录
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"zhangsan\",\"password\":\"123456\"}"

# 当前用户（把 <token> 换成登录返回的 token）
curl http://localhost:8080/api/auth/me -H "Authorization: Bearer <token>"
```

## 目录结构

```
src/main/java/com/repair
├── common      # 统一返回 / 错误码 / 分页 / 异常 / 枚举
├── config      # MyBatis-Plus / Security / CORS / JWT 属性
├── security    # JWT 工具 / 认证过滤器 / 当前用户
├── entity      # 数据库实体
├── mapper      # MyBatis-Plus Mapper
├── dto         # 请求 / 响应对象
├── service     # 业务逻辑（含初始化）
└── controller  # 仅接收参数与返回结果
```

## 生产注意事项

- `JWT_SECRET`、`DB_USERNAME`、`DB_PASSWORD` 必须用环境变量覆盖默认值；
- `app.cors.allowed-origin-patterns` 上线收紧为具体前端域名；
- 开发期 SQL 日志（StdOutImpl）上线改为 `org.apache.ibatis.logging.nologging.NoLoggingImpl`。
