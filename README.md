# PIXORA 智能云图库

PIXORA 是一个前后端分离的智能云图库项目，围绕图片上传、审核、检索、详情和个人空间建立完整工作流。

当前仓库的正式前端入口是 `picture-frontend`，默认打开：

```text
http://localhost:5173/gallery
```

## 与旧版 README 的主要变化

旧版 README 主要记录 Spring Boot 3.5.16 迁移基线，没有覆盖当前前端已经实现的页面和业务流程。当前代码已经补齐：

- 统一的 PIXORA 顶部导航和页面外框，正式页面共用 `PixoraLayout`。
- 公共图库的关键词、分类、标签、排序、分页和图片详情。
- 登录、注册、退出登录、用户中心、个人资料和头像上传。
- 本地多图上传、网络地址上传、公共图库/私人空间选择和上传结果明细。
- 我的图片筛选、编辑、重新上传、删除和分页管理。
- 私人空间创建、重命名、容量统计、等级展示、图片管理和删除。
- 管理员用户管理、图片审核、批量抓图和空间运营。

## 功能与路由

所有正式页面都使用同一个外部框架和导航栏；权限由前端路由守卫与后端 Session 共同校验。

| 路由 | 功能 | 权限 |
| --- | --- | --- |
| `/gallery` | 公共图库：搜索、分类、标签、排序、分页、图片卡片 | 公开 |
| `/gallery/detail/:id` | 图片预览、尺寸/格式/大小/比例、分类、标签、审核信息 | 后端按图片可见性校验 |
| `/gallery/upload` | 本地文件或网络地址上传，填写图片信息并查看批量结果 | 登录用户 |
| `/gallery/manage` | 管理自己的公共图片或私人空间图片 | 登录用户 |
| `/user/login` | 登录并支持登录后回跳原地址 | 公开 |
| `/user/register` | 注册账号、密码确认、性别和手机号校验 | 公开 |
| `/user/center` | 查看资料、角色、图片统计、最近上传，编辑资料和头像 | 登录用户 |
| `/space` | 创建/重命名/删除私人空间，查看容量和图片 | 登录用户 |
| `/admin/users` | 用户查询、新增、编辑和删除 | 管理员 |
| `/admin/pictures/review` | 按审核状态查看图片，单张或批量通过/拒绝 | 管理员 |
| `/admin/pictures/import` | 按搜索词批量抓图并查看入库结果 | 管理员 |
| `/admin/spaces` | 查看空间使用情况并调整空间等级 | 管理员 |

### 上传约束

- 支持 JPG、JPEG、PNG、WEBP。
- 单个文件最大 5MB，批量上传合计最大 30MB。
- 普通用户上传公共图片后进入审核流程；管理员上传按当前代码自动通过。
- 上传到私人空间前必须先创建私人空间。

### 兼容入口

- `/prototype/*` 保留为设计对照入口，使用独立的原型外框，不是正式产品入口。
- `/picture`、`/picture/upload`、`/picture/detail/:id`、`/picture/manage` 等旧地址会跳转到新路由，避免旧书签失效。

## 技术栈

### 后端

- Java 17
- Spring Boot 3.5.16
- MyBatis-Plus 3.5.17
- MySQL
- Redis：Session、缓存
- 腾讯云 COS：图片对象存储
- Springdoc 2.8.17 + Knife4j 4.5.0 纯 UI
- Caffeine 本地缓存

后端保持前后端分离，未将前端静态资源放入 Spring Boot。当前 M0 基线尚未接入 Spring AI。

### 前端

- Vue 3 + TypeScript + Vite
- Vue Router：正式路由、旧地址兼容跳转和权限元信息
- Pinia：登录用户状态
- Axios：统一 API 请求和 Session 凭证
- Ant Design Vue：表单、表格、分页、上传、弹窗等交互组件
- OpenAPI 生成前端 API 类型与请求封装
- 共享布局：`src/layout/PixoraLayout.vue` 和 `src/layout/pixora-layout.css`
- 视觉字体：英文与数字重点使用 `Abril Fatface`，功能性文字使用 `Manrope`

## 项目结构

```text
PIXORA/
├─ picture-backend/                 # Java + Spring Boot 后端
│  ├─ src/main/java/.../Controller/ # 用户、图片、空间、文件接口
│  ├─ src/main/java/.../Service/    # 业务服务
│  └─ .env.example                  # 后端环境变量示例
├─ picture-frontend/                # Vue 3 前端
│  ├─ src/layout/                   # 正式页面共享外框
│  ├─ src/config/route.ts           # 正式路由、原型路由和兼容跳转
│  ├─ src/pages/prototype/          # 当前实际页面组件
│  ├─ src/api/                      # OpenAPI 生成并维护的请求封装
│  └─ .env.example                  # Vite API 地址示例
├─ PRODUCT.md                       # 产品目标和设计原则
├─ HANDOFF.md                       # 技术基线与交接信息
└─ BUG_BACKLOG.md                   # 已审计问题和后续验证项
```

## 本地运行

### 1. 准备依赖

- JDK 17
- Maven
- Node.js 和 npm
- MySQL，默认数据库名为 `pictureSystem`
- Redis，默认地址为 `localhost:6379`
- 腾讯云 COS 配置（上传和对象存储功能需要）

### 2. 启动后端

在 `picture-backend/.env.example` 的基础上准备本地环境变量，至少配置 MySQL、Redis 和 COS。当前开发环境推荐使用 VS Code 启动配置的 `envFile` 注入 `picture-backend/.env`；直接使用 Maven 时，也需要把同名变量注入当前进程。

```bash
cd picture-backend
mvn spring-boot:run
```

后端默认端口为 `8123`，健康检查地址：

```text
http://localhost:8123/health/isSuccess
```

### 3. 启动前端

复制 `picture-frontend/.env.example` 为本地 `.env`，确认 `VITE_API_BASE_URL` 指向后端地址。

```bash
cd picture-frontend
npm install
npm run dev
```

前端默认端口为 `5173`，启动后访问 `http://localhost:5173/gallery`。

## 常用命令

在 `picture-frontend` 目录执行：

```bash
npm run dev             # 启动开发服务器
npm run build           # Vite 生产构建
npm run build:typecheck # TypeScript 检查后再构建
npm run preview        # 预览构建产物
npm run openapi         # 后端 OpenAPI 可访问时重新生成 API 封装
```

在 `picture-backend` 目录执行：

```bash
mvn test
mvn package
```

本地接口文档由 Springdoc 生成，常用地址为 `/v3/api-docs` 和 `/doc.html`；生产配置默认关闭接口文档。

## 当前边界

- 图片详情页的“保存到我的空间”按钮目前只是禁用的功能预留，尚未接入复制图片接口。
- Spring AI 尚未接入，当前系统仍是传统图片管理、审核和空间运营流程。
- `npm run build` 是当前前端主要构建验证；完整 `vue-tsc` 检查仍有已记录的共享请求类型问题，详见 `BUG_BACKLOG.md`。

## 仓库维护

后续仅维护 GitHub 上的 `yke6519-droid/PIXORA`。原 Gitee 仓库只作为历史快照保留，不再同步、提交或发布。

真实密码、云密钥、Session 配置和部署凭据不得提交到 Git。
