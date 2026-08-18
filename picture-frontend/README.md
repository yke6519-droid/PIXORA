# PIXORA 前端

PIXORA 前端使用 Vue 3、TypeScript 和 Vite，当前正式入口是：

```text
http://localhost:5173/gallery
```

## 页面能力

- `/gallery`：公共图片搜索、分类、标签、排序、分页和详情跳转。
- `/gallery/detail/:id`：图片预览、元数据、审核状态和上传者信息。
- `/gallery/upload`：本地多图或网络地址上传，支持公共图库/私人空间目标和结果明细。
- `/gallery/manage`：图片筛选、编辑、重新上传、删除和分页。
- `/space`：私人空间创建、重命名、容量统计和图片管理。
- `/user/login`、`/user/register`、`/user/center`：登录、注册、资料和头像管理。
- `/admin/*`：管理员用户管理、图片审核、批量抓图和空间运营。

正式路由共用 `src/layout/PixoraLayout.vue`；`/prototype/*` 仅作为设计对照入口。旧 `/picture/*` 地址保留兼容跳转。

## 开发

先配置 `picture-frontend/.env`：

```dotenv
VITE_API_BASE_URL=http://localhost:8123
```

然后执行：

```bash
npm install
npm run dev
```

常用命令：

```bash
npm run build
npm run build:typecheck
npm run preview
npm run openapi
```

`npm run openapi` 要求后端已启动，并能访问 `http://localhost:8123/v3/api-docs`。

## 代码入口

```text
src/config/route.ts                         # 正式路由、原型路由、兼容跳转
src/layout/PixoraLayout.vue                 # 全局正式外框和导航
src/layout/pixora-layout.css                # 全局比例、导航和页面布局
src/pages/prototype/gallery/                # 图库、详情、上传、管理
src/pages/prototype/user/                   # 登录、注册、用户中心
src/pages/prototype/space/                  # 私人空间
src/pages/prototype/admin/                  # 管理员功能
src/api/                                    # OpenAPI 请求封装
src/stores/useLoginUserStore.ts             # 登录用户状态
```

前端使用 Axios 的 `withCredentials` 传递后端 Session；需要登录或管理员权限的页面由路由守卫和后端接口共同校验。

详情页“保存到我的空间”目前仍是禁用的预留按钮，尚未接入后端接口。
