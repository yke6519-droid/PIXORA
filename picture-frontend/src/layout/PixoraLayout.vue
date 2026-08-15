<template>
  <!-- 所有正式页面共用同一层外框，页面组件只负责自己的内容布局。 -->
  <a-layout class="prototype-app pixora-app">
    <a-layout-header class="pixora-header">
      <!-- 顶栏和正文共用同一条画布基线，切换页面时不会出现左右漂移。 -->
      <div class="pixora-header-inner">
        <RouterLink to="/gallery" class="pixora-brand" aria-label="进入 PIXORA 公共图库">
          <span class="brand-mark">P</span>
          <strong>PIXORA</strong>
        </RouterLink>

        <!-- 桌面端只保留最常用的业务入口，让图库继续占据主要视觉空间。 -->
        <nav class="pixora-primary-nav" aria-label="主导航">
          <RouterLink to="/gallery" class="pixora-nav-link" :class="{ 'is-active': isPrimaryNavActive('/gallery') }">公共图库</RouterLink>
          <RouterLink to="/gallery/upload" class="pixora-nav-link" :class="{ 'is-active': isPrimaryNavActive('/gallery/upload') }">上传图片</RouterLink>
          <RouterLink to="/gallery/manage" class="pixora-nav-link" :class="{ 'is-active': isPrimaryNavActive('/gallery/manage') }">我的图片</RouterLink>
          <RouterLink to="/space" class="pixora-nav-link" :class="{ 'is-active': isPrimaryNavActive('/space') }">个人空间</RouterLink>
        </nav>

        <div class="pixora-header-actions">
          <a-dropdown v-if="isAdmin" placement="bottomRight" :trigger="['click']">
            <a-button class="pixora-header-button" type="text">管理后台</a-button>
            <template #overlay>
              <a-menu @click="handleAdminMenuClick">
                <a-menu-item key="/admin/users">用户管理</a-menu-item>
                <a-menu-item key="/admin/pictures/review">图片审核</a-menu-item>
                <a-menu-item key="/admin/pictures/import">批量抓图</a-menu-item>
                <a-menu-item key="/admin/spaces">空间运营</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>

          <a-dropdown v-if="loginUser" placement="bottomRight" :trigger="['click']">
            <button class="pixora-account" type="button" aria-label="打开账户菜单">
              <a-avatar :size="32" :src="loginUser.avatarurl">
                {{ accountFallback }}
              </a-avatar>
              <span>{{ loginUser.username || loginUser.useraccount || '我的账户' }}</span>
            </button>
            <template #overlay>
              <a-menu @click="handleAccountMenuClick">
                <a-menu-item key="/user/center">用户中心</a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout">退出登录</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>

          <a-button v-else class="pixora-login-button" @click="router.push('/user/login')">
            登录
          </a-button>

          <!-- 窄屏把所有入口收进一个菜单，避免顶栏挤压主内容。 -->
          <a-dropdown class="pixora-mobile-menu" placement="bottomRight" :trigger="['click']">
            <a-button class="pixora-header-button" type="text">菜单</a-button>
            <template #overlay>
              <a-menu @click="handleMobileMenuClick">
                <a-menu-item key="/gallery">公共图库</a-menu-item>
                <a-menu-item key="/gallery/upload">上传图片</a-menu-item>
                <a-menu-item key="/gallery/manage">我的图片</a-menu-item>
                <a-menu-item key="/space">个人空间</a-menu-item>
                <a-menu-divider />
                <a-menu-item v-if="loginUser" key="/user/center">用户中心</a-menu-item>
                <a-menu-item v-if="loginUser" key="logout">退出登录</a-menu-item>
                <a-menu-item v-else key="/user/login">登录</a-menu-item>
                <template v-if="isAdmin">
                  <a-menu-divider />
                  <a-menu-item key="/admin/users">用户管理</a-menu-item>
                  <a-menu-item key="/admin/pictures/review">图片审核</a-menu-item>
                  <a-menu-item key="/admin/pictures/import">批量抓图</a-menu-item>
                  <a-menu-item key="/admin/spaces">空间运营</a-menu-item>
                </template>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </div>
    </a-layout-header>

    <a-layout-content class="prototype-page pixora-page" :class="pageClasses">
      <RouterView />
    </a-layout-content>
  </a-layout>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { userLogout } from '../api/userController'
import { useLoginUserStore } from '../stores/useLoginUserStore'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const logoutLoading = ref(false)

const loginUser = computed(() => loginUserStore.loginUser)
const isAdmin = computed(() => loginUser.value?.userLevel === 'admin')
const accountFallback = computed(() =>
  (loginUser.value?.username || loginUser.value?.useraccount || '用').charAt(0),
)

// 页面高度策略由路由元信息统一管理，避免再次依赖具体 URL 字符串。
const pageClasses = computed(() => ({
  'prototype-page-fixed': Boolean(route.meta.fixed),
  'prototype-page-detail': Boolean(route.meta.detail),
  'prototype-page-upload': Boolean(route.meta.upload),
}))

function navigateByMenuKey(key: string) {
  if (key) void router.push(key)
}

function isPrimaryNavActive(path: string) {
  // 图片详情仍属于公共图库；上传和管理使用各自的独立激活状态。
  if (path === '/gallery') {
    return route.path === '/gallery' || route.path.startsWith('/gallery/detail/')
  }
  return route.path === path
}

function handleAdminMenuClick({ key }: { key: string }) {
  navigateByMenuKey(key)
}

async function handleAccountMenuClick({ key }: { key: string }) {
  if (key !== 'logout') {
    navigateByMenuKey(key)
    return
  }

  if (logoutLoading.value) return
  logoutLoading.value = true
  try {
    const response = await userLogout()
    if (response.data?.code !== 200) {
      throw new Error(response.data?.message || '退出登录失败')
    }
    loginUserStore.clearLoginUser()
    message.success('已退出登录')
    await router.replace('/gallery')
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '退出登录失败')
  } finally {
    logoutLoading.value = false
  }
}

function handleMobileMenuClick({ key }: { key: string }) {
  if (key === 'logout') {
    void handleAccountMenuClick({ key })
    return
  }
  navigateByMenuKey(key)
}
</script>

<style src="../pages/prototype/prototype.css"></style>
<style src="./pixora-layout.css"></style>
