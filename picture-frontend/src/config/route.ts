import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('../layout/PixoraLayout.vue'),
    children: [
      {
        path: '',
        redirect: '/gallery',
      },
      {
        path: 'gallery',
        name: 'gallery',
        component: () => import('../pages/prototype/gallery/GalleryPrototypePage.vue'),
      },
      {
        path: 'gallery/detail/:id',
        name: 'gallery-detail',
        component: () => import('../pages/prototype/gallery/GalleryDetailPrototypePage.vue'),
        meta: { fixed: true, detail: true },
      },
      {
        path: 'gallery/upload',
        name: 'gallery-upload',
        component: () => import('../pages/prototype/gallery/GalleryUploadPrototypePage.vue'),
        meta: { requiresAuth: true, upload: true },
      },
      {
        path: 'gallery/manage',
        name: 'gallery-manage',
        component: () => import('../pages/prototype/gallery/GalleryManagePrototypePage.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'user/login',
        name: 'user-login',
        component: () => import('../pages/prototype/user/UserLoginPrototypePage.vue'),
        meta: { fixed: true },
      },
      {
        path: 'user/register',
        name: 'user-register',
        component: () => import('../pages/prototype/user/UserRegisterPrototypePage.vue'),
        meta: { fixed: true },
      },
      {
        path: 'user/center',
        name: 'user-center',
        component: () => import('../pages/prototype/user/UserCenterPrototypePage.vue'),
        meta: { requiresAuth: true, fixed: true },
      },
      {
        path: 'space',
        name: 'space',
        component: () => import('../pages/prototype/space/SpacePrototypePage.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'admin/users',
        name: 'admin-users',
        component: () => import('../pages/prototype/admin/UserAdminPrototypePage.vue'),
        meta: { requiresAuth: true, requiresAdmin: true },
      },
      {
        path: 'admin/pictures/review',
        name: 'admin-picture-review',
        component: () => import('../pages/prototype/admin/PictureReviewPrototypePage.vue'),
        meta: { requiresAuth: true, requiresAdmin: true },
      },
      {
        path: 'admin/pictures/import',
        name: 'admin-picture-import',
        component: () => import('../pages/prototype/admin/PictureImportPrototypePage.vue'),
        meta: { requiresAuth: true, requiresAdmin: true, fixed: true },
      },
      {
        path: 'admin/spaces',
        name: 'admin-spaces',
        component: () => import('../pages/prototype/admin/SpaceAdminPrototypePage.vue'),
        meta: { requiresAuth: true, requiresAdmin: true },
      },
    ],
  },

  // 原型首页暂时保持原样，作为设计对照，不参与正式前端入口。
  {
    path: '/prototype',
    component: () => import('../pages/prototype/PrototypeLayout.vue'),
    children: [
      { path: '', redirect: '/prototype/home' },
      { path: 'home', component: () => import('../pages/prototype/PrototypeHomePage.vue') },
      { path: 'gallery', component: () => import('../pages/prototype/gallery/GalleryPrototypePage.vue') },
      { path: 'gallery/detail/:id', component: () => import('../pages/prototype/gallery/GalleryDetailPrototypePage.vue') },
      { path: 'gallery/upload', component: () => import('../pages/prototype/gallery/GalleryUploadPrototypePage.vue') },
      { path: 'gallery/manage', component: () => import('../pages/prototype/gallery/GalleryManagePrototypePage.vue') },
      { path: 'user/login', component: () => import('../pages/prototype/user/UserLoginPrototypePage.vue') },
      { path: 'user/register', component: () => import('../pages/prototype/user/UserRegisterPrototypePage.vue') },
      { path: 'user/center', component: () => import('../pages/prototype/user/UserCenterPrototypePage.vue') },
      { path: 'space', component: () => import('../pages/prototype/space/SpacePrototypePage.vue') },
      { path: 'admin/users', component: () => import('../pages/prototype/admin/UserAdminPrototypePage.vue') },
      { path: 'admin/pictures/review', component: () => import('../pages/prototype/admin/PictureReviewPrototypePage.vue') },
      { path: 'admin/pictures/import', component: () => import('../pages/prototype/admin/PictureImportPrototypePage.vue') },
      { path: 'admin/spaces', component: () => import('../pages/prototype/admin/SpaceAdminPrototypePage.vue') },
    ],
  },

  // 旧前端地址只做兼容跳转，避免书签失效，不再加载旧页面实现。
  { path: '/picture', redirect: '/gallery' },
  { path: '/picture/upload', redirect: '/gallery/upload' },
  {
    path: '/picture/detail/:id',
    redirect: (to) => ({ name: 'gallery-detail', params: { id: to.params.id } }),
  },
  { path: '/picture/manage', redirect: '/gallery/manage' },
  { path: '/userManage', redirect: '/admin/users' },
  { path: '/userIndex', redirect: '/user/center' },
  { path: '/admin/management', redirect: '/admin/users' },
  { path: '/my-space', redirect: '/space' },
  { path: '/:pathMatch(.*)*', redirect: '/gallery' },
]

export default routes
