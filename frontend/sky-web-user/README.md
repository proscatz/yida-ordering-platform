# 驿达点餐网页用户端

面向预约点餐与履约管理场景的独立 Web 用户端，采用 Vue 3、Vite、TypeScript、Pinia、Vue Router、Axios 和 Vant。界面移动端优先，并为桌面浏览器提供居中双栏布局。

## 本地运行

需要 Node.js 18+。先启动后端服务（默认 `http://localhost:8080`），再运行：

```powershell
cd frontend/sky-web-user
npm install
npm run dev
```

浏览器访问 `http://localhost:5173`。

## 环境变量与接口代理

复制 `.env.example` 为 `.env.local` 后可覆盖默认值：

```dotenv
VITE_API_TARGET=http://localhost:8080
VITE_PAYMENT_PROVIDER=mock
```

开发环境中的 `/api/*` 请求由 Vite 代理到 `VITE_API_TARGET`，转发前会移除 `/api` 前缀。例如 `/api/user/dish/list` 会请求后端 `/user/dish/list`。生产环境可由网关提供同样的 `/api` 反向代理规则，或通过部署配置调整请求基地址。

网页端使用用户名或手机号加密码登录，不依赖微信 `code`。登录 Token 通过 `authentication` 请求头发送；遇到 401 会统一清理会话并跳转登录页。

## 支付适配

默认 `mock` 适配器调用后端的本地模拟支付接口，不依赖真实商户环境。真实支付入口已保留，可在应用启动时调用 `registerRealPaymentInvoker` 注册真实支付唤起逻辑，并将 `VITE_PAYMENT_PROVIDER` 改为 `real`。真实支付实现应只接收后端返回的支付参数，不能在浏览器端保存商户密钥。

## 构建与预览

```powershell
npm run build
npm run preview
```

构建产物输出到 `dist`。提交代码时不要提交 `node_modules`、`dist` 或本机 `.env.local`。
