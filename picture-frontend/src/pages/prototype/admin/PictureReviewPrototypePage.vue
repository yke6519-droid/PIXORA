<template>
  <div class="review-prototype">
    <section class="proto-page-head">
      <div>
        <span class="proto-eyebrow">图片审核 / queryAll + adminCheckPicture</span>
        <h1 class="proto-title">先看清内容，<br />再做审核决定。</h1>
        <p class="proto-copy">审核页使用管理员专用 `queryAll`，返回原始 Picture 分页；通过或拒绝分别提交 `checkResult`，拒绝时必须记录 `checkMessage`。</p>
      </div>
      <div class="review-counter"><span>待处理</span><strong>{{ pendingPictures.length }}</strong><small>pictureCheck = 0</small></div>
    </section>

    <section class="review-toolbar proto-section">
      <div class="review-tabs"><button v-for="tab in tabs" :key="tab.value" :class="{ active: activeTab === tab.value }" @click="activeTab = tab.value">{{ tab.label }}<b>{{ tab.value === 'all' ? prototypePictures.length : prototypePictures.filter((picture) => tab.value === 'pending' ? picture.pictureCheck === 0 : tab.value === 'pass' ? picture.pictureCheck === 1 : picture.pictureCheck === 2).length }}</b></button></div>
      <div class="review-batch proto-surface"><div class="proto-flex proto-gap-12"><a-checkbox :checked="allSelected" @change="toggleAll" /><span>选择当前列表</span></div><div class="review-batch-actions"><a-button class="proto-button ghost-button" :disabled="!selectedIds.length" @click="batchPass">批量通过</a-button><a-button class="proto-button ghost-button" :disabled="!selectedIds.length" @click="batchRefuse">批量拒绝</a-button></div></div>
    </section>

    <section class="review-grid">
      <article v-for="picture in visiblePictures" :key="picture.id" class="review-card proto-surface proto-rounded">
        <div class="review-card-image proto-image-wrap"><img :src="picture.thumbnailUrl" :alt="picture.name" /><div class="review-image-check"><a-checkbox :checked="selectedIds.includes(picture.id)" @change="(event: any) => toggleOne(picture.id, event.target.checked)" /></div></div>
        <div class="review-card-body"><div class="review-card-title"><strong>{{ picture.name }}</strong><a-tag class="proto-status" :class="statusClass(picture.pictureCheck)">{{ pictureStatusText(picture.pictureCheck) }}</a-tag></div><p>{{ picture.introduction }}</p><div class="review-meta"><span>{{ picture.createdUser.username }}</span><span>{{ picture.createtime }}</span></div><div class="review-card-actions"><a-button class="proto-button ghost-button" @click="openReview(picture)">查看审核</a-button><a-button v-if="picture.pictureCheck === 0" class="proto-button acid-button" type="primary" @click="passOne(picture)">快速通过</a-button></div><div v-if="picture.pictureCheck === 2" class="review-message">{{ picture.checkMessage }}</div></div>
      </article>
    </section>

    <a-empty v-if="!visiblePictures.length" description="当前状态没有图片" />
    <a-modal v-model:open="reviewOpen" :title="reviewPicture ? `审核图片 #${reviewPicture.id}` : '审核图片'" ok-text="提交审核" cancel-text="取消" @ok="submitReview">
      <div v-if="reviewPicture" class="review-modal-preview"><img :src="reviewPicture.url" :alt="reviewPicture.name" /><div><strong>{{ reviewPicture.name }}</strong><p>{{ reviewPicture.introduction }}</p><span class="proto-mono">{{ reviewPicture.category }} / {{ reviewPicture.tags.join(' · ') }}</span></div></div>
      <a-form layout="vertical" class="proto-form"><a-form-item label="checkResult"><a-radio-group v-model:value="reviewResult"><a-radio :value="1">审核通过</a-radio><a-radio :value="2">审核拒绝</a-radio></a-radio-group></a-form-item><a-form-item v-if="reviewResult === 2" label="checkMessage"><a-textarea v-model:value="checkMessage" :rows="4" placeholder="请输入拒绝原因" /></a-form-item></a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { prototypePictures, pictureStatusText, type PrototypePicture } from '../prototypeData'

const tabs = [{ label: '全部图片', value: 'all' }, { label: '待审核', value: 'pending' }, { label: '审核通过', value: 'pass' }, { label: '审核拒绝', value: 'refuse' }] as const
const activeTab = ref<(typeof tabs)[number]['value']>('pending')
const selectedIds = ref<number[]>([])
const reviewOpen = ref(false)
const reviewPicture = ref<PrototypePicture | null>(null)
const reviewResult = ref<1 | 2>(1)
const checkMessage = ref('')
const pendingPictures = computed(() => prototypePictures.filter((picture) => picture.pictureCheck === 0))
const visiblePictures = computed(() => prototypePictures.filter((picture) => activeTab.value === 'all' || activeTab.value === 'pending' && picture.pictureCheck === 0 || activeTab.value === 'pass' && picture.pictureCheck === 1 || activeTab.value === 'refuse' && picture.pictureCheck === 2))
const allSelected = computed(() => visiblePictures.value.length > 0 && visiblePictures.value.every((picture) => selectedIds.value.includes(picture.id)))
function statusClass(status: number) { return status === 1 ? 'pass' : status === 2 ? 'refuse' : 'wait' }
function toggleOne(id: number, checked: boolean) { selectedIds.value = checked ? [...new Set([...selectedIds.value, id])] : selectedIds.value.filter((item) => item !== id) }
function toggleAll(event: any) { selectedIds.value = event.target.checked ? visiblePictures.value.map((picture) => picture.id) : [] }
function openReview(picture: PrototypePicture) { reviewPicture.value = picture; reviewResult.value = picture.pictureCheck === 2 ? 2 : 1; checkMessage.value = picture.checkMessage || ''; reviewOpen.value = true }
function passOne(picture: PrototypePicture) { message.success(`原型演示：adminCheckPicture / picId=${picture.id} / checkResult=1`) }
function batchPass() { message.success(`原型演示：adminCheckPictureBatch / picIds=${selectedIds.value.join(',')} / checkResult=1`) }
function batchRefuse() { reviewPicture.value = null; reviewResult.value = 2; reviewOpen.value = true }
function submitReview() { message.success(`原型演示：adminCheckPicture${reviewPicture.value ? '' : 'Batch'} / checkResult=${reviewResult.value}${reviewResult.value === 2 ? ` / checkMessage=${checkMessage.value || '未填写'}` : ''}`); reviewOpen.value = false; checkMessage.value = '' }
</script>

<style scoped>
.review-counter { min-width: 160px; padding: 17px; background: var(--proto-ink); color: var(--proto-paper); }
.review-counter span, .review-counter strong, .review-counter small { display: block; }
.review-counter span { color: var(--proto-orange); font-family: 'DM Mono', monospace; font-size: 10px; }
.review-counter strong { margin-top: 24px; color: var(--proto-acid); font-size: 47px; line-height: .8; letter-spacing: -.1em; }
.review-counter small { margin-top: 15px; color: rgba(241,242,237,.5); font-family: 'DM Mono', monospace; font-size: 9px; }
.review-toolbar { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; }
.review-tabs { display: flex; gap: 3px; flex-wrap: wrap; }
.review-tabs button { min-height: 52px; padding: 0 16px; border: 1px solid var(--proto-line); background: transparent; color: var(--proto-muted); cursor: pointer; font-family: 'Manrope', sans-serif; font-size: 11px; }
.review-tabs button b { margin-left: 8px; color: var(--proto-orange); font-family: 'DM Mono', monospace; font-size: 10px; font-weight: 400; }
.review-tabs button.active { border-color: var(--proto-ink); background: var(--proto-ink); color: var(--proto-paper); }
.review-batch { padding: 10px 12px; display: flex; align-items: center; gap: 14px; }
.review-batch-actions { display: flex; gap: 7px; }
.review-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.review-card { overflow: hidden; }
.review-card-image { height: 218px; position: relative; }
.review-image-check { position: absolute; top: 13px; left: 13px; padding: 4px; background: rgba(241,242,237,.8); }
.review-card-body { padding: 16px; }
.review-card-title { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.review-card-title strong { font-size: 16px; letter-spacing: -.04em; }
.review-card-body p { min-height: 35px; margin: 9px 0 12px; color: var(--proto-muted); font-size: 11px; line-height: 1.6; }
.review-meta { display: flex; justify-content: space-between; color: var(--proto-muted); font-family: 'DM Mono', monospace; font-size: 9px; }
.review-card-actions { display: flex; gap: 7px; margin-top: 16px; }
.review-card-actions .proto-button { flex: 1; padding-inline: 8px; font-size: 11px; }
.review-message { margin-top: 13px; padding: 8px 10px; border-left: 2px solid var(--proto-orange); background: rgba(255,137,106,.12); color: var(--proto-muted); font-size: 10px; }
.review-modal-preview { display: grid; grid-template-columns: 135px 1fr; gap: 13px; margin-bottom: 22px; }
.review-modal-preview img { width: 135px; height: 105px; object-fit: cover; }
.review-modal-preview strong { font-size: 15px; }
.review-modal-preview p { margin: 8px 0; color: var(--proto-muted); font-size: 11px; line-height: 1.5; }
.review-modal-preview span { color: var(--proto-orange); font-size: 9px; }
@media (max-width: 950px) { .review-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .review-toolbar { align-items: flex-start; flex-direction: column; } }
@media (max-width: 600px) { .review-grid { grid-template-columns: 1fr; } .review-tabs button { padding-inline: 10px; } .review-batch { align-items: flex-start; flex-direction: column; } }
</style>
