<template>
  <div class="import-prototype">
    <section class="proto-page-head">
      <div>
        <span class="proto-eyebrow">批量抓图 / adminFetchPictureBatch</span>
        <h1 class="proto-title">让管理员的<br />批量动作更有节奏。</h1>
        <p class="proto-copy">当前后端从 Bing 异步拉取图片，管理员提交搜索词、数量、名称、分类和标签；数量上限为 20。</p>
      </div>
      <div class="import-limit"><span>单次上限</span><strong>20</strong><small>count</small></div>
    </section>

    <section class="import-layout proto-section">
      <div class="import-form-card proto-surface proto-rounded">
        <div class="form-card-heading"><span class="proto-eyebrow">batch request</span><h2>配置抓取任务</h2></div>
        <a-form layout="vertical" class="proto-form" @finish="submitImport">
          <a-form-item label="搜索词 searchText" required><a-input v-model:value="form.searchText" placeholder="例如：minimal architecture" /></a-form-item>
          <div class="import-two-col"><a-form-item label="数量 count"><a-input-number v-model:value="form.count" :min="1" :max="20" style="width: 100%" /></a-form-item><a-form-item label="默认名称 name"><a-input v-model:value="form.name" placeholder="为空时使用 searchText" /></a-form-item></div>
          <a-form-item label="分类 category"><a-select v-model:value="form.category" style="width: 100%"><a-select-option v-for="category in categories.slice(1)" :key="category" :value="category">{{ category }}</a-select-option></a-select></a-form-item>
          <a-form-item label="标签 tags"><div class="import-tags"><a-checkable-tag v-for="tag in tags" :key="tag" :checked="form.tags.includes(tag)" @change="(checked: boolean) => toggleTag(tag, checked)">{{ tag }}</a-checkable-tag></div></a-form-item>
          <div class="import-contract"><span>接口将为每张图片补充</span><strong>introduction = searchText + 相关图片</strong></div>
          <a-button html-type="submit" class="proto-button acid-button import-submit" type="primary">开始批量抓取</a-button>
        </a-form>
      </div>
      <div class="import-preview">
        <div class="import-preview-head"><span class="proto-eyebrow">preview / static</span><h2>任务结果预览</h2></div>
        <div class="import-preview-list"><article v-for="picture in previewPictures" :key="picture.id" class="import-preview-item"><div class="import-preview-image proto-image-wrap"><img :src="picture.thumbnailUrl" :alt="picture.name" /></div><div><strong>{{ picture.name }}</strong><span>{{ form.category }} · {{ form.tags.join(' / ') || '未选标签' }}</span><small>上传后状态：管理员自动审核通过</small></div></article></div>
        <div class="import-result"><span>最近一次执行</span><strong>{{ importedMessage }}</strong></div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { prototypeCategories, prototypePictures, prototypeTags } from '../prototypeData'

const categories = prototypeCategories
const tags = prototypeTags.slice(0, 7)
const form = reactive({ searchText: 'minimal architecture', count: 6, name: '', category: '建筑', tags: ['建筑'] })
const importedMessage = ref('等待执行')
const previewPictures = computed(() => prototypePictures.slice(0, Math.min(form.count, 4)))
function toggleTag(tag: string, checked: boolean) { if (checked) form.tags.push(tag); else form.tags = form.tags.filter((item) => item !== tag) }
function submitImport() { importedMessage.value = `已抓取 ${form.count} 张，管理员自动通过`; message.success(`原型已提交：adminFetchPictureBatch / count=${form.count}`) }
</script>

<style scoped>
.import-prototype { height: 100%; min-height: 0; display: flex; flex-direction: column; }
.import-prototype > .proto-page-head { padding-top: 0; gap: 14px; }
.import-prototype > .proto-page-head .proto-title { margin-top: 7px; font-size: clamp(30px, 3.5vw, 50px); }
.import-prototype > .proto-page-head .proto-copy { max-width: 500px; font-size: 11px; line-height: 1.45; }
.import-limit { min-width: 130px; padding: 15px; background: var(--proto-acid); }
.import-limit span, .import-limit strong, .import-limit small { display: block; }
.import-limit span { font-family: 'DM Mono', monospace; font-size: 10px; }
.import-limit strong { margin-top: 14px; font-size: 49px; line-height: .8; letter-spacing: -.1em; }
.import-limit small { margin-top: 11px; font-family: 'DM Mono', monospace; font-size: 10px; opacity: .55; }
.import-layout.proto-section { flex: 1 1 auto; min-height: 0; padding-top: 10px; overflow: hidden; }
.import-layout { display: grid; grid-template-columns: minmax(400px, .85fr) minmax(360px, 1.15fr); grid-template-rows: minmax(0, 1fr); gap: 12px; }
.import-form-card { height: 100%; min-height: 0; display: flex; flex-direction: column; padding: clamp(14px, 2.1vw, 23px); }
.import-form-card :deep(.ant-form-item) { margin-bottom: 7px; }
.form-card-heading { margin-bottom: 8px; }
.form-card-heading h2, .import-preview-head h2 { margin: 6px 0 10px; font-size: 27px; letter-spacing: -.07em; }
.import-two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 13px; }
.import-tags :deep(.ant-tag-checkable) { padding: 4px 9px; border-radius: 3px; font-size: 11px; }
.import-tags :deep(.ant-tag-checkable-checked) { background: var(--proto-acid); color: var(--proto-ink); }
.import-contract { display: flex; flex-direction: column; gap: 4px; margin: 10px 0; padding: 9px; border-left: 2px solid var(--proto-orange); background: rgba(255,137,106,.12); color: var(--proto-muted); font-family: 'DM Mono', monospace; font-size: 9px; }
.import-contract strong { color: var(--proto-ink); font-weight: 500; }
.import-submit { width: 100%; }
.import-preview { min-height: 0; height: 100%; display: flex; flex-direction: column; padding: 2px 0; overflow: visible; }
.import-preview-head h2 { margin-bottom: 10px; }
.import-preview-list { flex: 1 1 auto; min-height: 0; display: grid; grid-template-columns: 1fr 1fr; grid-template-rows: repeat(2, minmax(0, 1fr)); gap: 8px; }
.import-preview-item { min-height: 0; padding: 6px; display: flex; flex-direction: column; gap: 4px; background: rgba(255,255,255,.5); border: 1px solid var(--proto-line); }
.import-preview-image { height: 62px; flex: 0 0 62px; }
.import-preview-item strong, .import-preview-item span, .import-preview-item small { display: block; }
.import-preview-item strong { font-size: 12px; }
.import-preview-item span { margin-top: 3px; color: var(--proto-orange); font-family: 'DM Mono', monospace; font-size: 9px; }
.import-preview-item small { margin-top: 3px; color: var(--proto-muted); font-size: 9px; }
.import-result { flex: 0 0 auto; margin-top: 7px; padding: 9px; display: flex; align-items: center; justify-content: space-between; gap: 10px; background: var(--proto-ink); color: var(--proto-paper); }
.import-result span { color: rgba(241,242,237,.57); font-family: 'DM Mono', monospace; font-size: 9px; }
.import-result strong { color: var(--proto-acid); font-size: 13px; font-weight: 500; }
@media (max-width: 820px) { .import-prototype { height: auto; } .import-layout.proto-section { flex: none; padding-top: 18px; overflow: visible; } .import-layout { grid-template-columns: 1fr; } .import-form-card { height: auto; } .import-preview { overflow: visible; } }
@media (max-width: 520px) { .import-two-col, .import-preview-list { grid-template-columns: 1fr; } }
</style>
