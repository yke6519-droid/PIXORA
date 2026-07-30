# PIXORA

PIXORA 是一个前后端分离的智能云图库项目，采用 Monorepo 结构：

```text
PIXORA/
├─ picture-backend/   # Java 17 + Spring Boot 3.5.16
└─ picture-frontend/  # Vue 3
```

## 本地运行

1. 参考前、后端目录中的 `.env.example` 配置本地环境变量。
2. 在 `picture-backend` 中使用 JDK 17 运行后端。
3. 在 `picture-frontend` 中执行 `npm install` 和 `npm run dev`。

真实密码、云密钥和部署凭据不得提交到 Git。

## 仓库维护

本项目后续仅维护 GitHub 上的 `yke6519-droid/PIXORA`。原 Gitee 仓库只作为历史快照保留，不再同步、提交或发布。

## M0 说明

当前后端以 Spring Boot 3.5.16 作为迁移过渡基线，尚未引入 Spring AI。接口文档由 Springdoc 2.8.x 生成，Knife4j 4.5.0 仅提供 UI；生产环境默认关闭接口文档。
