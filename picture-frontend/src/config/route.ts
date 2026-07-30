const routes:any = [
    {
        path: '/prototype',
        component: ()=> import('../pages/prototype/PrototypeLayout.vue'),
        children: [
            {
                path: '',
                redirect: '/prototype/home',
            },
            {
                path: 'home',
                component: ()=> import('../pages/prototype/PrototypeHomePage.vue'),
            },
            {
                path: 'gallery',
                component: ()=> import('../pages/prototype/gallery/GalleryPrototypePage.vue'),
            },
            {
                path: 'gallery/detail/:id',
                component: ()=> import('../pages/prototype/gallery/GalleryDetailPrototypePage.vue'),
            },
            {
                path: 'gallery/upload',
                component: ()=> import('../pages/prototype/gallery/GalleryUploadPrototypePage.vue'),
            },
            {
                path: 'gallery/manage',
                component: ()=> import('../pages/prototype/gallery/GalleryManagePrototypePage.vue'),
            },
            {
                path: 'user/login',
                component: ()=> import('../pages/prototype/user/UserLoginPrototypePage.vue'),
            },
            {
                path: 'user/register',
                component: ()=> import('../pages/prototype/user/UserRegisterPrototypePage.vue'),
            },
            {
                path: 'user/center',
                component: ()=> import('../pages/prototype/user/UserCenterPrototypePage.vue'),
            },
            {
                path: 'space',
                component: ()=> import('../pages/prototype/space/SpacePrototypePage.vue'),
            },
            {
                path: 'admin/users',
                component: ()=> import('../pages/prototype/admin/UserAdminPrototypePage.vue'),
            },
            {
                path: 'admin/pictures/review',
                component: ()=> import('../pages/prototype/admin/PictureReviewPrototypePage.vue'),
            },
            {
                path: 'admin/pictures/import',
                component: ()=> import('../pages/prototype/admin/PictureImportPrototypePage.vue'),
            },
            {
                path: 'admin/spaces',
                component: ()=> import('../pages/prototype/admin/SpaceAdminPrototypePage.vue'),
            },
        ],
    },
    {
        path:'/',
        component: ()=> import('../layout/BasicLayout.vue'),
        children: [
            {
                path: '',
                redirect: '/picture'  // 默认跳转到图库页面
            },
            // 图片模块路由
            {
                path: 'picture',
                component: ()=> import('../pages/picture/PictureIndex.vue')
            },
            {
                path: 'picture/upload',
                component: ()=> import('../pages/picture/PictureUploadPage.vue')
            },
            {
                path: 'picture/detail/:id',
                component: ()=> import('../pages/picture/PictureDetailPage.vue')
            },
            {
                path: 'picture/manage',
                component: ()=> import('../pages/picture/PictureManagePage.vue')
            },
            {
                path: 'userManage',
                component: ()=> import('../pages/admin/UserManagePage.vue')
            },
            {
                path: 'userIndex',
                component: ()=> import('../pages/user/UserIndex.vue')
            },
            {
                path:'user/login',
                component: ()=> import('../pages/user/UserLoginPage.vue')
            },
            {
                path:'user/register',
                component: ()=> import('../pages/user/UserRegisterPage.vue')
            },
            {
                path: 'admin/management',
                component: ()=> import('../pages/admin/UserManagePage.vue')
            },
            {
                path: 'my-space',
                component: ()=> import('../pages/space/MySpace.vue')
            },
        ]
    },
]

export default routes
