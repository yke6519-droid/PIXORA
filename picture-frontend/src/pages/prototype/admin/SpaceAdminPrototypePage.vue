<template>
  <div class="space-admin-prototype">
    <section class="proto-page-head">
      <div>
        <span class="proto-eyebrow">空间运营 / querySpacePage + alterLevelById</span>
        <h1 class="proto-title">空间不是黑盒，<br />容量与等级都可读。</h1>
        <p class="proto-copy">管理员可以查询所有空间、查看创建者和使用量，并将空间等级调整为 0、1、2。页面不扩展当前的一人一空间模型。</p>
      </div>
      <div class="space-admin-summary"><span>空间总数</span><strong>{{ prototypeSpaces.length }}</strong><small>querySpacePage</small></div>
    </section>

    <section class="space-admin-metrics proto-section"><div class="admin-metric proto-surface"><span>总图片数量</span><strong>58</strong><small>usedCount</small></div><div class="admin-metric proto-surface"><span>容量占用</span><strong>4.0 MB</strong><small>usedSize</small></div><div class="admin-metric proto-surface"><span>需要关注</span><strong>1</strong><small>超过 75%</small></div></section>

    <section class="space-admin-table proto-section">
      <div class="space-admin-table-head"><div><span class="proto-eyebrow">spaces</span><h2 class="proto-subtitle">私人空间清单</h2></div><a-select v-model:value="levelFilter" style="width: 140px"><a-select-option value="all">全部等级</a-select-option><a-select-option :value="0">基础空间</a-select-option><a-select-option :value="1">专业空间</a-select-option><a-select-option :value="2">专家空间</a-select-option></a-select></div>
      <div class="space-admin-list"><article v-for="space in filteredSpaces" :key="space.id" class="space-admin-row"><div class="space-admin-name"><span class="space-id">#{{ space.id }}</span><strong>{{ space.spaceName }}</strong><small>{{ space.createdUser }} · {{ space.createTime }}</small></div><div class="space-admin-usage"><div class="usage-bar"><i :style="{ width: `${Math.round(space.usedSize / space.maxSize * 100)}%` }"></i></div><span>{{ formatSize(space.usedSize) }} / {{ formatSize(space.maxSize) }}</span></div><div class="space-admin-level"><a-tag class="proto-tag acid-tag">{{ formatSpaceLevel(space.spaceLevel) }}</a-tag><small>level {{ space.spaceLevel }}</small></div><div class="space-admin-count"><strong>{{ space.usedCount }}</strong><span>/ {{ space.maxCount }} 张</span></div><a-button class="proto-button ghost-button" @click="openLevel(space)">调整等级</a-button></article></div>
    </section>

    <a-modal v-model:open="levelOpen" title="调整空间等级" ok-text="保存等级" cancel-text="取消" @ok="saveLevel"><a-form layout="vertical" class="proto-form"><a-form-item label="spaceId"><a-input :value="selectedSpace?.id" disabled /></a-form-item><a-form-item label="alterLevel"><a-select v-model:value="selectedLevel" style="width: 100%"><a-select-option :value="0">0 / 基础空间 · 1MB · 50张</a-select-option><a-select-option :value="1">1 / 专业空间 · 5MB · 100张</a-select-option><a-select-option :value="2">2 / 专家空间 · 10MB · 200张</a-select-option></a-select></a-form-item></a-form></a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { formatSize, formatSpaceLevel, prototypeSpaces } from '../prototypeData'

const levelFilter = ref<'all' | 0 | 1 | 2>('all')
const levelOpen = ref(false)
const selectedSpace = ref<(typeof prototypeSpaces)[number] | null>(null)
const selectedLevel = ref<0 | 1 | 2>(1)
const filteredSpaces = computed(() => prototypeSpaces.filter((space) => levelFilter.value === 'all' || space.spaceLevel === levelFilter.value))
function openLevel(space: (typeof prototypeSpaces)[number]) { selectedSpace.value = space; selectedLevel.value = space.spaceLevel as 0 | 1 | 2; levelOpen.value = true }
function saveLevel() { message.success(`原型已提交：alterLevelById / spaceId=${selectedSpace.value?.id} / alterLevel=${selectedLevel.value}`); levelOpen.value = false }
</script>

<style scoped>
.space-admin-summary { min-width: 160px; padding: 18px; background: var(--proto-blue); }
.space-admin-summary span, .space-admin-summary strong, .space-admin-summary small { display: block; }
.space-admin-summary span { font-family: 'DM Mono', monospace; font-size: 10px; }
.space-admin-summary strong { margin-top: 20px; font-size: 55px; line-height: .8; letter-spacing: -.1em; }
.space-admin-summary small { margin-top: 15px; font-family: 'DM Mono', monospace; font-size: 9px; opacity: .52; }
.space-admin-metrics { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.admin-metric { min-height: 122px; padding: 18px; display: flex; flex-direction: column; justify-content: space-between; }
.admin-metric span { color: var(--proto-muted); font-size: 11px; }
.admin-metric strong { font-size: 35px; letter-spacing: -.08em; }
.admin-metric small { color: var(--proto-orange); font-family: 'DM Mono', monospace; font-size: 9px; }
.space-admin-table-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 15px; margin-bottom: 25px; }
.space-admin-table-head h2 { margin-top: 9px; }
.space-admin-list { border-top: 2px solid var(--proto-ink); }
.space-admin-row { min-height: 111px; padding: 17px 0; display: grid; grid-template-columns: 1.2fr 1fr .7fr .6fr 118px; gap: 18px; align-items: center; border-bottom: 1px solid var(--proto-line); }
.space-id { display: block; color: var(--proto-orange); font-family: 'DM Mono', monospace; font-size: 10px; }
.space-admin-name strong, .space-admin-name small { display: block; }
.space-admin-name strong { margin-top: 7px; font-size: 16px; letter-spacing: -.04em; }
.space-admin-name small { margin-top: 6px; color: var(--proto-muted); font-size: 10px; }
.space-admin-usage span { display: block; margin-top: 7px; color: var(--proto-muted); font-family: 'DM Mono', monospace; font-size: 9px; }
.usage-bar { height: 5px; overflow: hidden; background: var(--proto-paper-deep); }
.usage-bar i { display: block; height: 100%; background: var(--proto-acid); }
.space-admin-level small { display: block; margin-top: 5px; color: var(--proto-muted); font-family: 'DM Mono', monospace; font-size: 9px; }
.space-admin-count strong { font-size: 25px; letter-spacing: -.08em; }
.space-admin-count span { color: var(--proto-muted); font-size: 10px; }
@media (max-width: 950px) { .space-admin-row { grid-template-columns: 1fr 1fr .7fr 95px; } .space-admin-row > .proto-button { grid-column: 4; grid-row: 1 / span 2; } }
@media (max-width: 650px) { .space-admin-metrics { grid-template-columns: 1fr; } .space-admin-table-head { align-items: flex-start; flex-direction: column; } .space-admin-row { grid-template-columns: 1fr 1fr; gap: 12px; } .space-admin-row > .proto-button { grid-column: 2; grid-row: auto; } }
</style>
