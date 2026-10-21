# 报修工单系统项目规则

## 技术栈约束
- 后端：Spring Boot 3.x + JDK 17 + MySQL 8 + MyBatis-Plus
- 前端：Vue 3 + Element Plus (Web端) + uni-app (移动端)
- 统一返回格式 `Result<T>` (包含 code, msg, data)

## 代码规范
- Controller 不写业务逻辑，只负责接收请求和返回结果。
- 状态流转必须在 Service 层统一处理。
- 每次状态变更，必须往 `notification` 表插入一条记录。

## 交互原则
- 如果需求不明确，请先向我提问，不要自行脑补复杂逻辑。

## 语言约束
- 无论内部思考（Thinking）使用何种语言，给用户的最终回复、实施计划、解释说明以及代码中的注释，必须 100% 使用简体中文输出。