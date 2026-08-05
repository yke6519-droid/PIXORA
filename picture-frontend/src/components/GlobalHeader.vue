<template>
  <div id="globalHeader">
    <a-row :wrap="false" align="middle" class="header-row">
      <!-- Logo区域 -->
      <a-col flex="220px">
        <router-link to="/">
          <div class="logo-box">
            <img class="logo" src="../assets/hero.png" alt="logo">
            <div class="title">智能云图库</div>
          </div>
        </router-link>
      </a-col>
      
      <!-- 导航菜单 -->
      <a-col flex="auto">
        <a-menu 
          class="headerMenu" 
          v-model:selectedKeys="current" 
          mode="horizontal" 
          :items="Items" 
          @click="doMenuClick"
        />
      </a-col>
      
      <!-- 用户操作区 -->
      <a-col flex="160px">
        <div class="userLoginStatus">
          <!-- 未登录 -->
          <a-button 
            v-if="!loginUserStore.loginUser" 
            type="primary" 
            @click="router.push('/user/login')"
          >
            登录
          </a-button>
          
          <!-- 已登录 -->
          <div v-else class="showUserInfo">
            <a-dropdown placement="bottomRight" arrow>
              <div class="user-dropdown-trigger" @click.prevent>
                <a-avatar 
                  :size="32" 
                  :src="loginUserStore.loginUser.avatarurl" 
                  v-if="loginUserStore.loginUser.avatarurl"
                />
                <a-avatar :size="32" v-else>
                  {{ loginUserStore.loginUser.username?.charAt(0).toUpperCase() || 'U' }}
                </a-avatar>
                <span class="username">{{ loginUserStore.loginUser.username ?? '用户' }}</span>
                <DownOutlined class="dropdown-icon" />
              </div>
              
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="toUserIndex">
                    <UserOutlined />
                    个人中心
                  </a-menu-item>
                  <a-menu-item @click="goToPictureManage">
                    <PictureOutlined />
                    我的图片
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item @click="Logout" danger>
                    <LogoutOutlined />
                    退出登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script lang="ts" setup>
import { h, onMounted, ref, computed } from 'vue';
import { 
  PictureOutlined, 
  DownOutlined,
  UserOutlined,
  LogoutOutlined
} from '@ant-design/icons-vue';
import type { MenuProps } from 'ant-design-vue';
import { useRouter } from 'vue-router';
import { useLoginUserStore } from '../stores/useLoginUserStore';
import { message } from 'ant-design-vue';

const loginUserStore = useLoginUserStore();
const router = useRouter();

/**
 * 导航菜单项（动态计算：管理员才显示"用户管理"）
 */
const Items = computed<MenuProps['items']>(() => {
  const baseItems: MenuProps['items'] = [
    {
      key: '/picture',
      icon: () => h(PictureOutlined),
      label: '图库',
      title: '图库',
    },
  ];

  // 仅管理员显示"用户管理"
  if (loginUserStore.loginUser?.userLevel === 'admin') {
    baseItems.push({
      key: '/admin/management',
      label: '用户管理',
      title: '用户管理',
    });
  }

  // 仅管理员显示"用户管理"
  if (loginUserStore.loginUser?.userLevel === 'admin') {
    baseItems.push({
      key: '/picture/manage',
      label: '图片管理',
      title: '图片管理',
    });
  }
  // 图片管理
  if (loginUserStore.loginUser?.userLevel != 'admin') {
    baseItems.push({
      key: '/picture/manage',
      label: '图片管理',
      title: '图片管理',
    });
  }
  // 我的空间
  baseItems.push({
      key: '/my-space',
      label: '我的空间',
      title: '我的空间',
    });
  return baseItems;
});

/**
 * 跳转用户中心页
 */
const toUserIndex = () => {
  router.push('/userIndex');
};

/**
 * 跳转图片管理页
 */
const goToPictureManage = () => {
  router.push('/picture/manage');
};

/**
 * 菜单选项跳转
 */
const doMenuClick: MenuProps['onClick'] = (e) => {
  router.push(e.key as string);
};

/**
 * 当前要高亮的菜单项
 */
const current = ref<string[]>([]);
const setActiveMenu = () => {
  current.value = [router.currentRoute.value.path];
};
router.afterEach(() => {
  setActiveMenu();
});
setActiveMenu();

/**
 * 登出
 */
import { userLogoutUsingGet } from '../api/userController';
const Logout = async () => {
  try {
    const res = await userLogoutUsingGet();
    if (res.data.code === 200) {
      loginUserStore.clearLoginUser();
      message.success('已成功退出登录');
      router.push('/');
    } else {
      message.error('退出登录失败');
    }
  } catch (error) {
    message.error('服务器异常，请重试');
  }
};

onMounted(async () => {
  await loginUserStore.fetchLoginUser();
});
</script>

<style scoped>
#globalHeader {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  height: 64px;
  line-height: 64px;
}

.header-row {
  height: 100%;
  padding: 0 24px;
  max-width: 1600px;
  margin: 0 auto;
}

/* Logo 容器 */
.logo-box {
  display: flex;
  align-items: center;
  height: 64px;
  text-decoration: none;
}

.logo {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 8px;
}

.title {
  margin-left: 12px;
  font-size: 18px;
  font-weight: 600;
  color: #1890ff;
  white-space: nowrap;
}

/* 导航菜单 */
.headerMenu {
  font-size: 15px;
  border-bottom: none;
  line-height: 62px;
  background: transparent;
}

.headerMenu :deep(.ant-menu-item) {
  padding: 0 20px;
  margin: 0 4px;
}

/* 用户登录状态 */
.userLoginStatus {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  height: 64px;
}

.showUserInfo {
  display: flex;
  align-items: center;
}

.user-dropdown-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.3s;
}

.user-dropdown-trigger:hover {
  background: #f5f5f5;
}

.username {
  font-size: 14px;
  color: #333;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dropdown-icon {
  font-size: 12px;
  color: #999;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .header-row {
    padding: 0 12px;
  }
  
  .title {
    display: none;
  }
  
  .username {
    display: none;
  }
}
</style>
