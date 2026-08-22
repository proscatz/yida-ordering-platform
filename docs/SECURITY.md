# 安全与脱敏说明

## 不应提交的内容

- `application-dev.yml`、`application-local.yml`、`.env` 等本机配置
- 数据库、Redis、RabbitMQ、JWT、OSS、微信或支付密钥
- 私钥、证书、签名文件和云厂商访问令牌
- IDE 数据源配置、日志、抓包、完整请求头和测试截图中的 Token
- 真实姓名、手机号、身份证、地址、OpenID、订单和支付回调报文

## 本地配置方式

公开的 `application.yml` 只引用环境变量。可以参考 `application-example.yml` 设置本机值，但不要把真实值写回受 Git 跟踪的文件。

如果任何凭据曾进入 Git 提交，即使随后删除文件，也应立即在对应服务端吊销并轮换凭据；仅增加 `.gitignore` 不能清除 Git 历史。

## 发布前检查

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-publication.ps1
git status --short
git diff --cached --name-only
```

检查脚本只输出规则名和文件路径，不输出命中的原始敏感文本。

