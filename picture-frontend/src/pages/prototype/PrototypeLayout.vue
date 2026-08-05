<template>
  <a-layout class="prototype-app">
    <a-layout-sider
      class="prototype-sidebar"
      :width="246"
      :collapsed-width="0"
      :trigger="null"
    >
      <RouterLink to="/prototype" class="prototype-brand">
        <span class="brand-mark">P</span>
        <span>
          <strong>PIXORA</strong>
        </span>
      </RouterLink>

      <nav class="prototype-nav" aria-label="主导航">
        <div class="nav-group">
          <span class="nav-group-title">浏览与创作</span>
          <RouterLink to="/prototype/gallery" class="prototype-nav-item">
            <span>公共图库</span>
          </RouterLink>
          <RouterLink to="/prototype/gallery/detail/2082749059889754114" class="prototype-nav-item">
            <span>图片详情</span>
          </RouterLink>
          <RouterLink to="/prototype/gallery/upload" class="prototype-nav-item">
            <span>上传图片</span>
          </RouterLink>
          <RouterLink to="/prototype/gallery/manage" class="prototype-nav-item">
            <span>图片管理</span>
          </RouterLink>
        </div>

        <div class="nav-group">
          <span class="nav-group-title">账户与空间</span>
          <RouterLink to="/prototype/user/login" class="prototype-nav-item">
            <span>登录 / 注册</span>
          </RouterLink>
          <RouterLink to="/prototype/user/center" class="prototype-nav-item">
            <span>用户中心</span>
          </RouterLink>
          <RouterLink to="/prototype/space" class="prototype-nav-item">
            <span>个人空间</span>
          </RouterLink>
        </div>

        <div class="nav-group">
          <span class="nav-group-title">管理后台</span>
          <RouterLink to="/prototype/admin/users" class="prototype-nav-item">
            <span>用户管理</span>
          </RouterLink>
          <RouterLink to="/prototype/admin/pictures/review" class="prototype-nav-item">
            <span>图片审核</span>
          </RouterLink>
          <RouterLink to="/prototype/admin/pictures/import" class="prototype-nav-item">
            <span>批量抓图</span>
          </RouterLink>
          <RouterLink to="/prototype/admin/spaces" class="prototype-nav-item">
            <span>空间运营</span>
          </RouterLink>
        </div>
      </nav>

    </a-layout-sider>

    <a-layout class="prototype-main">
      <a-layout-header class="prototype-topbar">
        <div class="topbar-route">
          <span class="topbar-slash">/</span>
          <span>{{ routeLabel }}</span>
        </div>
      </a-layout-header>

      <a-layout-content class="prototype-page" :class="prototypePageClasses">
        <RouterView />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const isFixedPage = computed(() =>
  route.path.includes('/prototype/gallery/detail/') ||
  route.path === '/prototype/user/login' ||
  route.path === '/prototype/user/register' ||
  route.path === '/prototype/user/center' ||
  route.path === '/prototype/admin/pictures/import'
)

// 只有图片详情需要严格锁定在一屏内；其他紧凑页允许在矮视口中自然增长，避免底部操作被裁切。
const prototypePageClasses = computed(() => ({
  'prototype-page-fixed': isFixedPage.value,
  'prototype-page-detail': route.path.includes('/prototype/gallery/detail/'),
  'prototype-page-upload': route.path === '/prototype/gallery/upload',
}))

const routeLabel = computed(() => {
  const path = route.path
  if (path.includes('/gallery/detail')) return '图片详情'
  if (path.includes('/gallery/upload')) return '上传图片'
  if (path.includes('/gallery/manage')) return '图片管理'
  if (path === '/prototype/gallery') return '公共图库'
  if (path.includes('/user/login')) return '登录'
  if (path.includes('/user/register')) return '注册'
  if (path.includes('/user/center')) return '用户中心'
  if (path.includes('/space')) return '个人空间'
  if (path.includes('/admin/users')) return '用户管理'
  if (path.includes('/admin/pictures/review')) return '图片审核'
  if (path.includes('/admin/pictures/import')) return '批量抓图'
  if (path.includes('/admin/spaces')) return '空间运营'
  return '项目总览'
})
</script>

<style src="./prototype.css"></style>
