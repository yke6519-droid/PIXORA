<template>
  <div class="prototype-app">
    <aside class="prototype-sidebar">
      <RouterLink to="/prototype" class="prototype-brand">
        <span class="brand-mark">C</span>
        <span>
          <strong>CLOUD / PIC</strong>
          <small>静态功能原型</small>
        </span>
      </RouterLink>

      <nav class="prototype-nav" aria-label="原型页面导航">
        <div class="nav-group">
          <span class="nav-group-title">浏览与创作</span>
          <RouterLink to="/prototype/gallery" class="prototype-nav-item">
            <span>公共图库</span><em>01</em>
          </RouterLink>
          <RouterLink to="/prototype/gallery/detail/101" class="prototype-nav-item">
            <span>图片详情</span><em>02</em>
          </RouterLink>
          <RouterLink to="/prototype/gallery/upload" class="prototype-nav-item">
            <span>上传图片</span><em>03</em>
          </RouterLink>
          <RouterLink to="/prototype/gallery/manage" class="prototype-nav-item">
            <span>图片管理</span><em>04</em>
          </RouterLink>
        </div>

        <div class="nav-group">
          <span class="nav-group-title">账户与空间</span>
          <RouterLink to="/prototype/user/login" class="prototype-nav-item">
            <span>登录 / 注册</span><em>05</em>
          </RouterLink>
          <RouterLink to="/prototype/user/center" class="prototype-nav-item">
            <span>用户中心</span><em>06</em>
          </RouterLink>
          <RouterLink to="/prototype/space" class="prototype-nav-item">
            <span>个人空间</span><em>07</em>
          </RouterLink>
        </div>

        <div class="nav-group">
          <span class="nav-group-title">管理后台</span>
          <RouterLink to="/prototype/admin/users" class="prototype-nav-item">
            <span>用户管理</span><em>08</em>
          </RouterLink>
          <RouterLink to="/prototype/admin/pictures/review" class="prototype-nav-item">
            <span>图片审核</span><em>09</em>
          </RouterLink>
          <RouterLink to="/prototype/admin/pictures/import" class="prototype-nav-item">
            <span>批量抓图</span><em>10</em>
          </RouterLink>
          <RouterLink to="/prototype/admin/spaces" class="prototype-nav-item">
            <span>空间运营</span><em>11</em>
          </RouterLink>
        </div>
      </nav>

      <div class="prototype-sidebar-foot">
        <div class="status-light"><i></i> 静态数据预览</div>
        <span>接口接入时保留字段映射</span>
      </div>
    </aside>

    <main class="prototype-main">
      <header class="prototype-topbar">
        <div class="topbar-route">
          <span class="topbar-slash">/</span>
          <span>{{ routeLabel }}</span>
        </div>
        <div class="topbar-actions">
          <a-tag color="green">Prototype</a-tag>
          <a-button type="text" class="topbar-exit" @click="router.push('/prototype')">回到总览</a-button>
        </div>
      </header>

      <div class="prototype-page" :class="{ 'prototype-page-fixed': isFixedPage }">
        <RouterView />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const isFixedPage = computed(() =>
  route.path.includes('/prototype/gallery/detail/') ||
  route.path === '/prototype/gallery/upload' ||
  route.path === '/prototype/user/login' ||
  route.path === '/prototype/user/register' ||
  route.path === '/prototype/user/center' ||
  route.path === '/prototype/admin/pictures/import'
)

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
