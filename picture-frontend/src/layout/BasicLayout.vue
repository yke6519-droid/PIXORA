<template>
  <div id="basicLayout">
    <a-layout style="min-height: 100vh">
      <!-- 头部 -->
      <a-layout-header class="header">
        <GlobalHeader />
      </a-layout-header>
      
      <!-- 内容区 -->
      <a-layout-content class="content">
        <router-view />
      </a-layout-content>
      
      <!-- 底部 -->
      <a-layout-footer class="footer">
        <a-space>
          <span>© 2026 智能云图库</span>
          <a href="https://github.com" target="_blank">GitHub</a>
        </a-space>
      </a-layout-footer>
    </a-layout>
  </div>
</template>

<script setup lang="ts">
import GlobalHeader from '../components/GlobalHeader.vue'
</script>

<style scoped>
#basicLayout {
  min-height: 100vh;
}

.header {
  background: #fff;
  padding: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.content {
  background: #f5f5f5;
  min-height: calc(100vh - 128px);
}

.footer {
  background: #fff;
  text-align: center;
  padding: 16px;
}
</style>
