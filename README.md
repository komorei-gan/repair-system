# 社区 / 校园报修工单系统

> 基于 **Spring Boot 3 + Vue 3 + uni-app** 的全栈校园 / 社区报修工单系统，覆盖「报修人提交 → 管理员派单 → 维修师傅接单 → 完成 → 报修人确认评价」的完整闭环，并内置仲裁、信誉分风控与多端通知。

<p align="center">
  <img src="https://img.shields.io/badge/Backend-Spring%20Boot%203.3.5-brightgreen" alt="Spring Boot 3.3.5" />
  <img src="https://img.shields.io/badge/JDK-17-blue" alt="JDK 17" />
  <img src="https://img.shields.io/badge/Web-Vue%203%20%2B%20Element%20Plus-409EFF" alt="Vue 3 + Element Plus" />
  <img src="https://img.shields.io/badge/Mobile-uni--app%20%2B%20wot--ui-2f54eb" alt="uni-app + wot-ui" />
  <img src="https://img.shields.io/badge/DB-MySQL%208-4479A1" alt="MySQL 8" />
</p>

---

## 📖 项目简介

一套面向校园 / 社区场景的报修工单系统，提供 **Web 管理端 / 维修端** 与 **移动端（uni-app H5）** 三端能力。报修人在移动端提交故障，管理员集中派单，维修师傅接单并完工，报修人确认评价——全流程状态流转严格收敛在服务层，每步状态变更均产生操作日志与站内通知，形成可审计、可追溯的工单闭环。

---

## ✨ 功能特性

- **🔐 RBAC 角色权限**：`ADMIN`（管理员）/ `REPAIRER`（维修师傅）/ `REPORTER`（报修人）三角色，基于 Spring Security + JWT 无状态鉴权，接口按角色隔离。
- **🔁 完整状态机流转**：`PENDING → ASSIGNED → PROCESSING → COMPLETED → CONFIRMED`，含 `CANCELLED` / `REJECTED` 终态及改派、仲裁等旁路流转；所有流转收敛在 Service 层并带行锁 + 条件更新双重并发保护。
- **📱 多端互动闭环**：报修端（uni-app H5）、维修端（Web）、管理端（Web）三端数据实时同步，站内通知贯穿全程。
- **⚖️ 仲裁与信誉分风控**：师傅可对恶意报修发起仲裁，管理员仲裁通过后自动扣减报修人信誉分，低于阈值自动禁用，遏制滥用。
- **🌙 深色模式**：Web 端与移动端均支持深色主题切换。
- **🔔 通知中心**：工单状态变更自动推送站内通知，未读红点实时同步。
- **📊 数据看板与导出**：管理端可视化看板（趋势 / 状态分布 / 关键指标）+ 工单 Excel 一键导出。
- **🛡️ 安全加固**：用户输入的标题 / 描述经 HTML 标签过滤，防存储型 XSS；关键写操作按钮防重复点击。

---

## 🧱 技术栈

### 后端（`src/`）

| 技术 | 版本 / 说明 |
| --- | --- |
| Spring Boot | 3.3.5（JDK 17） |
| MySQL | 8.x（utf8mb4，自动建库） |
| MyBatis-Plus | 3.5.7（Spring Boot 3 专用 starter） |
| Spring Security + JWT | jjwt 0.12.6，无状态鉴权 |
| EasyExcel | 3.3.4（工单 / 报表导出） |
| Knife4j | 4.5.0（在线接口文档，自带 springdoc） |
| Hutool | 5.8.32（HTML 过滤等工具） |

### Web 管理端 / 维修端（`frontend/`）

| 技术 | 说明 |
| --- | --- |
| Vue | 3.5.x |
| Vite | 8.x |
| Element Plus | 2.14.x |
| Pinia | 4.x（状态管理） |
| Vue Router | 4.x |
| ECharts | 6.x（数据可视化） |

### 移动端（`mobile/`）

| 技术 | 说明 |
| --- | --- |
| uni-app | 3.x（Vue 3 语法，支持 H5 / 小程序 / App 多端） |
| wot-ui v2 | 移动端 UI 组件库 |
| Vue | 3.4.x |

---

## 📁 项目结构

```
repair-system/
├── src/                          # 后端（Spring Boot 3）
│   ├── main/java/com/repair/
│   │   ├── controller/           # 控制器层（只接收请求、返回结果）
│   │   ├── service/              # 业务层（状态流转、XSS 过滤等）
│   │   ├── mapper/               # MyBatis-Plus Mapper
│   │   ├── entity/               # 实体
│   │   ├── dto/                  # 请求 / 响应 DTO
│   │   ├── config/               # 配置（安全 / CORS / Knife4j / MyBatis-Plus）
│   │   ├── security/             # JWT 工具与过滤器
│   │   └── common/               # 统一返回、异常、枚举、工具
│   └── main/resources/
│       ├── application.yml       # 主配置（数据库 / JWT / 上传目录）
│       └── sql/schema.sql        # 建表脚本（启动时幂等执行）
├── frontend/                     # Web 管理端 / 维修端（Vue 3 + Element Plus）
│   └── src/
│       ├── views/                # 页面（看板 / 工单 / 派单 / 用户 / 仲裁 / 审核…）
│       ├── components/           # 公共组件（派单弹窗 / 图表 / 骨架屏…）
│       ├── api/                  # 接口封装（axios）
│       ├── stores/               # Pinia 状态
│       └── layouts/              # 布局与顶栏 / 侧边栏
├── mobile/                       # 移动端（uni-app + wot-ui）
│   └── src/
│       ├── pages/                # 页面（登录 / 报修 / 工单 / 师傅工作台 / 个人中心）
│       ├── api/                  # 接口封装（uni.request）
│       └── utils/                # token / 主题 / 状态工具
├── uploads/                      # 报修图片存储目录
├── design.md                     # 核心设计文档（状态机 / 表结构）
└── README.md
```

---

## 🚀 快速启动指南

### 0. 环境要求

| 依赖 | 版本要求 |
| --- | --- |
| JDK | 17+ |
| Maven | 3.6+ |
| MySQL | 8.x |
| Node.js | 18+（建议 20+） |
| pnpm | 8+（也可用 npm / yarn） |

### 1. 后端启动

1. **准备数据库**：默认连接 `jdbc:mysql://localhost:3306/repair_system`，首次启动会通过 `createDatabaseIfNotExist=true` 自动建库，并由 `schema.sql` 幂等建表。
2. **配置环境变量**（可选，均有默认值）：

   | 环境变量 | 默认值 | 说明 |
   | --- | --- | --- |
   | `DB_USERNAME` | `root` | 数据库用户名 |
   | `DB_PASSWORD` | `mysql` | 数据库密码 |
   | `JWT_SECRET` | 内置开发密钥 | **生产环境务必覆盖**，长度 ≥ 32 字节 |

3. **启动**：

   ```bash
   # 项目根目录
   mvn spring-boot:run
   ```

   启动成功后接口地址为 `http://localhost:8080`，接口文档见 `http://localhost:8080/doc.html`（Knife4j）。

   > 本地开发可用 `--spring.profiles.active=dev` 激活 dev profile（额外输出 debug 日志，并启用 `zhangsan` 联调账号兜底重置）。种子数据由 `DataInitializer` 幂等创建（仅首次），可安全用于生产首次部署。

### 2. Web 管理端启动

```bash
cd frontend
pnpm install
pnpm run dev
```

访问 `http://localhost:5173`。开发环境接口地址由 `frontend/.env.development` 配置（默认 `http://localhost:8080`）。

### 3. 移动端 H5 启动

```bash
cd mobile
pnpm install
pnpm run dev:h5
```

访问 `http://localhost:5174`。接口地址在 `mobile/src/api/request.ts` 的 `BASE_URL`（默认 `http://localhost:8080`），如有需要请按环境修改。

---

## 👤 演示账号

| 角色 | 账号 | 密码 | 说明 |
| --- | --- | --- | --- |
| 管理员 | `admin` | `admin123` | 管理端登录，负责派单 / 审核 / 仲裁 |
| 维修师傅 | `master` | `master123` | 维修端登录，接单 / 完工 |
| 维修师傅 | `master2` | `master123` | 备用联调师傅 |
| 报修人 | `zhangsan` | `zhangsan123` | 移动端登录，提交 / 确认报修 |

> **⚠️ 部署至公网环境后请及时修改默认密码**

以上账号由后端启动时的种子数据初始化（幂等，仅首次创建）。`zhangsan` 的密码兜底重置仅在 `dev` profile 下执行，生产环境不会触发。请勿在公网直接使用默认口令。

---

## 🖼️ 项目截图

> **提示**：请在项目根目录创建 `docs/images` 文件夹，将对应截图放入后推送到 GitHub，即可在下方占位符位置展示。截图建议尺寸：PC 端 16:10，移动端 9:19.5。

| 截图 | 占位说明 |
| --- | --- |
| PC 端数据看板 | `![PC端数据看板](./docs/images/dashboard.png)` |
| PC 端工单列表 | `![PC端工单列表](./docs/images/order-list.png)` |
| 移动端首页 | `![移动端首页](./docs/images/mobile-home.png)` |
| 移动端工单详情 | `![移动端工单详情](./docs/images/mobile-detail.png)` |

![PC端数据看板](./docs/images/dashboard.png)

![PC端工单列表](./docs/images/order-list.png)

![移动端首页](./docs/images/mobile-home.png)

![移动端工单详情](./docs/images/mobile-detail.png)

---

## 📄 License

本项目仅供学习与演示使用。
