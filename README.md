# PIXORA

PIXORA 是一个前后端分离的智能云图库项目。本仓库采用 Monorepo 结构：

```text
PIXORA/
├─ picture-backend/   # Spring Boot 后端
└─ picture-frontend/  # Vue 3 前端
```

## 本地运行

1. 参考两个子项目中的 `.env.example` 配置本地环境变量。
2. 在 `picture-backend` 中启动后端服务。
3. 在 `picture-frontend` 中执行 `npm install` 和 `npm run dev`。

真实密码、云密钥和部署凭据不得提交到 Git。
