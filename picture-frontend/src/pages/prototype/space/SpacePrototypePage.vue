<template>
  <div class="space-prototype">
    <section class="proto-page-head">
      <div>
        <span class="proto-eyebrow">个人空间 / querySpaceById</span>
        <h1 class="proto-title">一个空间，<br />装下你的视觉秩序。</h1>
        <p class="proto-copy">当前空间模型是一人一个私人空间，页面围绕 SpaceVO 的容量、数量、等级和空间内图片展开。</p>
      </div>
      <div class="space-level-block"><span>当前等级</span><strong>{{ formatSpaceLevel(space.spaceLevel) }}</strong><small>level {{ space.spaceLevel }}</small></div>
    </section>

    <section class="space-overview proto-section">
      <div class="space-usage-card proto-surface proto-rounded">
        <div class="usage-card-head"><div><span class="proto-eyebrow">容量使用</span><h2>{{ space.spaceName }}</h2></div><a-button class="proto-button ghost-button" @click="renameOpen = true">重命名</a-button></div>
        <div class="usage-ring" :style="{ background: `conic-gradient(var(--proto-acid) ${sizePercent}%, var(--proto-paper-deep) 0)` }"><div class="usage-ring-inner"><strong>{{ sizePercent }}%</strong><span>usedSize</span></div></div>
        <div class="usage-values"><div><span>已使用空间</span><strong>{{ formatSize(space.usedSize) }} / {{ formatSize(space.maxSize) }}</strong></div><div><span>图片数量</span><strong>{{ space.usedCount }} / {{ space.maxCount }}</strong></div></div>
        <a-progress :percent="sizePercent" :show-info="false" stroke-color="#baff3d" trail-color="#e4e7df" />
        <div class="usage-foot"><span>空间创建者 {{ space.createdUser }}</span><span>最近更新 {{ space.updateTime }}</span></div>
      </div>
      <div class="space-side-stack"><div class="proto-bento-card dark"><span class="proto-eyebrow">空间权限</span><h3>只对你开放</h3><p>图片查询和管理会校验空间创建者、管理员与当前 Session。</p><span class="bento-corner">spaceId / {{ space.id }}</span></div><div class="proto-bento-card acid"><span class="proto-eyebrow">创建规则</span><h3>每位用户一个空间</h3><p>当前后端 createSpace 会限制重复创建。</p></div></div>
    </section>

    <section class="space-gallery proto-section">
      <div class="space-gallery-head"><div><span class="proto-eyebrow">空间图片 / queryPicturePageCache</span><h2 class="proto-subtitle">{{ space.usedCount }} 张图片正在这里。</h2></div><div class="proto-page-head-actions"><a-button class="proto-button ghost-button" @click="deleteOpen = true">删除空间</a-button><a-button class="proto-button acid-button" type="primary" @click="router.push('/prototype/gallery/upload')">上传到空间</a-button></div></div>
      <div class="space-picture-grid">
        <article v-for="picture in spacePictures" :key="picture.id" class="space-picture-card" @click="router.push(`/prototype/gallery/detail/${picture.id}`)"><div class="space-picture-image proto-image-wrap"><img :src="picture.thumbnailUrl" :alt="picture.name" /></div><div class="space-picture-copy"><strong>{{ picture.name }}</strong><span>{{ picture.category }} · {{ formatSize(picture.picsize) }}</span></div></article>
      </div>
    </section>

    <a-modal v-model:open="renameOpen" title="重命名空间" ok-text="保存名称" cancel-text="取消" @ok="renameSpace"><a-form layout="vertical" class="proto-form"><a-form-item label="updatedName"><a-input v-model:value="updatedName" /></a-form-item></a-form></a-modal>
    <a-modal v-model:open="deleteOpen" title="删除私人空间" ok-text="确认删除" cancel-text="取消" @ok="deleteSpace"><p class="proto-muted">原型演示删除动作，对应 `/space/deleteById?spaceId={{ space.id }}`。</p></a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { formatSize, formatSpaceLevel, prototypePictures, prototypeSpaces } from '../prototypeData'

const router = useRouter()
const space = prototypeSpaces[0]
const spacePictures = computed(() => prototypePictures.filter((picture) => picture.spaceId === space.id))
const sizePercent = computed(() => Math.round(space.usedSize / space.maxSize * 100))
const renameOpen = ref(false)
const deleteOpen = ref(false)
const updatedName = ref(space.spaceName)
function renameSpace() { message.success('原型已保存：updateById / updatedName'); renameOpen.value = false }
function deleteSpace() { message.warning('原型演示：deleteById / spaceId'); deleteOpen.value = false }
</script>

<style scoped>
.space-level-block { min-width: 180px; padding: 18px; background: var(--proto-orange); }
.space-level-block span, .space-level-block strong, .space-level-block small { display: block; }
.space-level-block span { font-family: 'DM Mono', monospace; font-size: 10px; }
.space-level-block strong { margin-top: 23px; font-size: 25px; letter-spacing: -.06em; }
.space-level-block small { margin-top: 7px; font-family: 'DM Mono', monospace; font-size: 10px; opacity: .58; }
.space-overview { display: grid; grid-template-columns: minmax(360px, 1.22fr) minmax(260px, .78fr); gap: 13px; align-items: stretch; }
.space-usage-card { padding: 27px; }
.usage-card-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 15px; }
.usage-card-head h2 { margin: 9px 0 0; font-size: 28px; letter-spacing: -.06em; }
.usage-ring { width: 184px; height: 184px; margin: 35px auto 26px; padding: 12px; display: grid; place-items: center; border-radius: 50%; }
.usage-ring-inner { width: 100%; height: 100%; display: grid; place-content: center; text-align: center; border-radius: 50%; background: var(--proto-paper); }
.usage-ring-inner strong, .usage-ring-inner span { display: block; }
.usage-ring-inner strong { font-size: 39px; letter-spacing: -.08em; }
.usage-ring-inner span { color: var(--proto-muted); font-family: 'DM Mono', monospace; font-size: 9px; }
.usage-values { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-bottom: 13px; }
.usage-values span, .usage-values strong { display: block; }
.usage-values span { color: var(--proto-muted); font-size: 10px; }
.usage-values strong { margin-top: 5px; font-size: 13px; }
.space-usage-card :deep(.ant-progress-inner) { border-radius: 0; }
.usage-foot { display: flex; justify-content: space-between; gap: 10px; margin-top: 17px; color: var(--proto-muted); font-family: 'DM Mono', monospace; font-size: 9px; }
.space-side-stack { display: grid; grid-template-rows: 1fr 1fr; gap: 13px; }
.space-side-stack .proto-bento-card { min-height: 0; }
.space-side-stack .proto-eyebrow { color: inherit; opacity: .55; }
.space-side-stack h3 { margin-top: 30px; }
.space-gallery-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 25px; }
.space-picture-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); grid-auto-flow: dense; gap: 11px; }
.space-picture-card { background: rgba(255,255,255,.5); border: 1px solid var(--proto-line); cursor: pointer; transition: transform .35s ease; }
.space-picture-card:hover { transform: translateY(-5px); }
.space-picture-image { height: 210px; }
.space-picture-copy { display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 13px; }
.space-picture-copy strong { font-size: 13px; }
.space-picture-copy span { color: var(--proto-muted); font-size: 10px; }
@media (max-width: 820px) { .space-overview { grid-template-columns: 1fr; } .space-gallery-head { align-items: flex-start; flex-direction: column; } }
@media (max-width: 580px) { .space-picture-grid { grid-template-columns: 1fr; } .usage-foot { align-items: flex-start; flex-direction: column; } }
</style>
