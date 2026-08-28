# 架构概览

## 应用组成

- `backend`：Maven 多模块工程，`sky-server` 为启动模块，`sky-common` 提供公共能力，`sky-pojo` 承载 DTO、VO 和实体。
- `frontend/sky-web-user`：面向顾客的移动优先网页端。
- `frontend/sky-web-admin`：面向员工的桌面管理端。

## 核心链路

用户通过 JWT 登录后完成浏览商品、购物车、地址选择和下单。服务端从购物车及当前商品价格重新计算金额，在同一事务内创建订单、订单明细、Outbox 事件并清空购物车。订单创建事件最终投递到 RabbitMQ，延迟消息负责超时关单，定时任务只承担补偿与对账。

管理端通过 Spring WebSocket 接收新订单和催单提醒。后端使用 `WebSocketConfigurer` 注册 `/ws/{sid}`，由 `HandshakeInterceptor` 完成 Token、员工 ID 与启用状态校验，再由单例 `TextWebSocketHandler` 管理会话、标准关闭帧和广播发送；同一员工的新连接会安全替换旧连接。前端负责指数退避重连、异常降级与重复消息去重。

Redis 用于菜品、套餐和店铺状态缓存，采用统一 Key、TTL 抖动、空值缓存、互斥重建和精确失效策略。
