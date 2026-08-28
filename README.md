# 驿达点餐

驿达点餐是一个面向预约点餐与履约管理场景的前后端分离项目，包含移动优先的用户端、桌面管理端和 Spring Boot 多模块后端。项目用于个人学习成果与求职作品展示，不提供线上商业服务。

## 技术栈

- 后端：Java 17、Spring Boot、Spring WebSocket、MyBatis、MySQL、Redis、RabbitMQ、JWT、JUnit 5、Mockito
- 管理端：Vue 3、Vite、TypeScript、Pinia、Vue Router、Axios、Element Plus、ECharts
- 用户端：Vue 3、Vite、TypeScript、Pinia、Vue Router、Axios、Vant
- 工程能力：BCrypt、订单状态机、幂等下单、Outbox、缓存治理、WebSocket 鉴权与重连、Mock 支付、OSS 上传、统一异常处理

## 仓库结构

```text
yida-ordering-platform/
├─ backend/                    Spring Boot Maven 多模块后端
├─ frontend/
│  ├─ sky-web-admin/           管理端（默认端口 5174）
│  └─ sky-web-user/            用户端（默认端口 5173）
├─ database/                   可公开的数据库增量脚本与说明
├─ docs/                       架构、安全与运行说明
└─ scripts/                    发布前脱敏检查
```

## 本地启动

准备 Java 17、Maven 3.9+、Node.js 20+、MySQL 8 和 Redis。RabbitMQ 默认关闭；需要演示订单延迟关单与 Outbox 时再通过环境变量启用。

1. 创建名为 `Yida` 的本地数据库并执行项目所需的基础表结构，再按需执行 `database/migrations` 中的公开增量脚本。
2. 参考 `backend/sky-server/src/main/resources/application-example.yml` 配置本机环境变量，任何真实密码或密钥都不要提交。
3. 启动后端：

```powershell
cd backend
mvn clean test
mvn -pl sky-server -am spring-boot:run
```

4. 启动用户端：

```powershell
cd frontend/sky-web-user
npm ci
npm run dev
```

5. 启动管理端：

```powershell
cd frontend/sky-web-admin
npm ci
npm run dev
```

用户端访问 `http://localhost:5173`，管理端访问 `http://localhost:5174`，两个 Vite 开发服务器默认把 API 代理到 `http://localhost:8080`。

## 配置安全

- 仓库不包含原项目的 `application-dev.yml`、`.env`、IDE 数据源、日志、证书、构建目录和 Nginx 运行目录。
- OSS、JWT、数据库、Redis、RabbitMQ、微信等配置均通过环境变量注入。
- 默认支付实现为 `mock`；真实支付参数不进入源码。
- 发布前执行 `powershell -ExecutionPolicy Bypass -File scripts/check-publication.ps1`。

更完整的配置与安全说明见 [docs/SECURITY.md](docs/SECURITY.md)。

## 说明

本仓库不附带真实用户数据、真实订单、支付回调报文或云服务凭据。数据库目录仅保留不涉及用户身份数据的增量脚本；原环境中的用户替换和地址演示脚本未进入公开副本。
