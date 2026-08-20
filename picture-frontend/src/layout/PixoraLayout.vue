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
                <a-menu-item key="/admin/avatars/review">头像审核</a-menu-item>
                <a-menu-item key="/admin/pictures/import">批量抓图</a-menu-item>
                <a-menu-item key="/admin/spaces">空间运营</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>

          <button
            v-if="loginUser"
            class="pixora-notification-trigger"
            :class="{ 'is-open': notificationOpen }"
            type="button"
            aria-label="打开通知"
            @click="handleNotificationOpen"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M18 9a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" />
            </svg>
            <span v-if="unreadNotificationCount > 0" class="pixora-notification-badge">
              {{ unreadNotificationCount > 99 ? '99+' : unreadNotificationCount }}
            </span>
          </button>

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
                  <a-menu-item key="/admin/avatars/review">头像审核</a-menu-item>
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

    <a-drawer
      v-model:open="notificationOpen"
      class="pixora-notification-drawer"
      :width="notificationDrawerWidth"
      placement="right"
      :mask="false"
      :destroy-on-close="false"
    >
      <template #title>
        <div class="pixora-notification-title">
          <span>通知</span>
          <span v-if="unreadNotificationCount > 0" class="pixora-notification-unread-count">
            {{ unreadNotificationCount }} 条未读
          </span>
        </div>
      </template>

      <div class="pixora-notification-content">
        <a-skeleton v-if="notificationLoading" active :paragraph="{ rows: 5 }" />

        <a-alert
          v-else-if="notificationError"
          type="error"
          show-icon
          :message="notificationError"
        >
          <template #action>
            <a-button size="small" @click="loadNotifications">重试</a-button>
          </template>
        </a-alert>

        <a-empty v-else-if="!notifications.length" description="暂时没有通知" />

        <div v-else class="pixora-notification-list">
          <article
            v-for="notification in notifications"
            :key="String(notification.id)"
            class="pixora-notification-item"
            :class="{ 'is-unread': !notification.readTime }"
          >
            <div class="pixora-notification-item-head">
              <div class="pixora-notification-item-heading">
                <span class="pixora-notification-status" aria-hidden="true"></span>
                <h3>{{ notification.title || '系统通知' }}</h3>
              </div>
              <time>{{ formatNotificationDate(notification.createTime) }}</time>
            </div>
            <p>
              <template v-if="notification.bizName && isPictureNotification(notification)">
                你的图片“<strong class="pixora-notification-biz-name">{{ notification.bizName }}</strong>”{{ pictureNotificationContent(notification) }}
              </template>
              <template v-else>{{ notification.content }}</template>
            </p>
            <div class="pixora-notification-item-actions">
              <a-button
                v-if="!notification.readTime"
                type="text"
                size="small"
                class="pixora-notification-action"
                :loading="readingNotificationId === String(notification.id)"
                @click="handleMarkNotificationRead(notification)"
              >
                标记已读
              </a-button>
              <RouterLink
                v-if="isPictureNotification(notification)"
                class="pixora-notification-action pixora-notification-view-link"
                :to="pictureDetailPath(notification)"
                @click="notificationOpen = false"
              >
                点击查看
              </RouterLink>
              <a-button
                type="text"
                size="small"
                class="pixora-notification-action pixora-notification-delete"
                :loading="deletingNotificationId === String(notification.id)"
                @click="handleDeleteNotification(notification)"
              >
                删除
              </a-button>
            </div>
          </article>
        </div>
      </div>
    </a-drawer>
  </a-layout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import {
  deleteNotifications,
  markNotificationsRead,
  queryNotificationPage,
} from '../api/notificationController'
import { userLogout } from '../api/userController'
import { useLoginUserStore } from '../stores/useLoginUserStore'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const logoutLoading = ref(false)
const notificationOpen = ref(false)
const notificationLoading = ref(false)
const notificationError = ref('')
const notifications = ref<API.NotificationVO[]>([])
const unreadNotificationCount = ref(0)
const deletingNotificationId = ref('')
const readingNotificationId = ref('')
const notificationDrawerWidth = 'min(380px, calc(100vw - 16px))'

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

/**
 * 浮窗打开时同时拉取通知列表和未读总数，避免只根据当前页数量显示角标。
 * 未读总数来自后端 readTime 字段，标记已读后重新拉取列表，保证角标和列表状态一致。
 */
async function loadNotifications() {
  if (!loginUser.value) return

  notificationLoading.value = true
  notificationError.value = ''
  try {
    const [pageResponse, unreadResponse] = await Promise.all([
      queryNotificationPage({ current: 1, pageSize: 20, unreadOnly: false }),
      queryNotificationPage({ current: 1, pageSize: 1, unreadOnly: true }),
    ])
    if (pageResponse.data?.code !== 200 || unreadResponse.data?.code !== 200) {
      throw new Error(pageResponse.data?.message || unreadResponse.data?.message || '通知加载失败')
    }
    notifications.value = pageResponse.data.data?.records || []
    unreadNotificationCount.value = Number(unreadResponse.data.data?.total || 0)
  } catch (error: any) {
    notificationError.value = error?.response?.data?.message || error?.message || '通知加载失败'
  } finally {
    notificationLoading.value = false
  }
}

function handleNotificationOpen() {
  notificationOpen.value = true
  void loadNotifications()
}

async function handleDeleteNotification(notification: API.NotificationVO) {
  if (!notification.id || deletingNotificationId.value) return

  const notificationId = String(notification.id)
  deletingNotificationId.value = notificationId
  try {
    const response = await deleteNotifications({ ids: [notification.id] })
    if (response.data?.code !== 200) {
      throw new Error(response.data?.message || '通知删除失败')
    }
    message.success('通知已删除')
    await loadNotifications()
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '通知删除失败')
  } finally {
    deletingNotificationId.value = ''
  }
}

async function handleMarkNotificationRead(notification: API.NotificationVO) {
  if (!notification.id || readingNotificationId.value) return

  const notificationId = String(notification.id)
  readingNotificationId.value = notificationId
  try {
    const response = await markNotificationsRead({ ids: [notification.id] })
    if (response.data?.code !== 200) {
      throw new Error(response.data?.message || '标记已读失败')
    }
    await loadNotifications()
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '标记已读失败')
  } finally {
    readingNotificationId.value = ''
  }
}

function isPictureNotification(notification: API.NotificationVO) {
  return notification.bizType === 'PICTURE_CHECK' && notification.bizId != null
}

function pictureDetailPath(notification: API.NotificationVO) {
  return `/gallery/detail/${String(notification.bizId)}`
}

function pictureNotificationContent(notification: API.NotificationVO) {
  const content = notification.content || ''
  return content.startsWith('你的图片') ? content.slice('你的图片'.length) : content
}

function formatNotificationDate(value?: string) {
  if (!value) return '—'
  return value.replace('T', ' ').replace(/\.\d+.*$/, '')
}

function resetNotifications() {
  notificationOpen.value = false
  notificationError.value = ''
  notifications.value = []
  unreadNotificationCount.value = 0
}

watch(
  () => loginUser.value?.id,
  (userId) => {
    if (userId) {
      void loadNotifications()
    } else {
      resetNotifications()
    }
  },
)

onMounted(() => {
  if (loginUser.value) {
    void loadNotifications()
  }
})
</script>

<style src="../pages/prototype/prototype.css"></style>
<style src="./pixora-layout.css"></style>
