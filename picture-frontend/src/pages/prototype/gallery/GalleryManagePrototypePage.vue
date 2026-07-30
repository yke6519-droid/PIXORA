<template>
  <div class="manage-prototype">
    <section class="proto-page-head">
      <div>
        <span class="proto-eyebrow">图片管理 / queryPicturePageCache</span>
        <h1 class="proto-title">把上传后的<br />每个状态看清。</h1>
        <p class="proto-copy">静态原型同时演示普通用户的“我的图片”和管理员的审核管理视图。切换右上角角色即可查看不同权限下的操作。</p>
      </div>
      <div class="role-switch"><span>当前视图</span><a-switch v-model:checked="isAdmin" checked-children="管理员" un-checked-children="用户" /></div>
    </section>

    <section class="manage-toolbar proto-surface proto-rounded proto-section">
      <div class="manage-toolbar-line">
        <a-input v-model:value="searchText" placeholder="按名称或简介搜索" allow-clear />
        <a-select v-model:value="status" style="width: 140px"><a-select-option value="all">全部状态</a-select-option><a-select-option :value="0">待审核</a-select-option><a-select-option :value="1">审核通过</a-select-option><a-select-option :value="2">审核拒绝</a-select-option></a-select>
        <a-select v-model:value="spaceScope" style="width: 160px"><a-select-option value="public">公共图库</a-select-option><a-select-option value="private">我的私人空间</a-select-option><a-select-option v-if="isAdmin" value="all-private">所有私人空间</a-select-option></a-select>
        <a-button class="proto-button ghost-button" @click="reset">重置</a-button>
      </div>
      <div class="manage-toolbar-foot"><span>当前结果 {{ filteredPictures.length }} 张</span><div class="manage-actions"><a-button class="proto-button ghost-button" :disabled="!selectedIds.length" @click="batchAction('pass')">批量通过</a-button><a-button class="proto-button ghost-button" :disabled="!selectedIds.length" @click="batchAction('refuse')">批量拒绝</a-button><a-button class="proto-button acid-button" type="primary" @click="router.push('/prototype/gallery/upload')">上传图片</a-button></div></div>
    </section>

    <section class="manage-table proto-section">
      <div class="manage-table-head"><div class="proto-flex proto-gap-12"><a-checkbox :checked="allSelected" @change="toggleAll" /><span>选择图片</span></div><span class="proto-mono">spaceScope / {{ spaceScope }}</span></div>
      <div class="manage-list">
        <article v-for="picture in filteredPictures" :key="picture.id" class="manage-row">
          <div class="manage-select"><a-checkbox :checked="selectedIds.includes(picture.id)" @change="(event: any) => toggleOne(picture.id, event.target.checked)" /></div>
          <div class="manage-thumb proto-image-wrap" @click="router.push(`/prototype/gallery/detail/${picture.id}`)"><img :src="picture.thumbnailUrl" :alt="picture.name" /></div>
          <div class="manage-main"><div class="manage-title-line"><strong>{{ picture.name }}</strong><a-tag class="proto-status" :class="statusClass(picture.pictureCheck)">{{ pictureStatusText(picture.pictureCheck) }}</a-tag></div><p>{{ picture.introduction }}</p><div class="manage-tags"><a-tag v-for="tag in picture.tags" :key="tag" class="proto-tag">{{ tag }}</a-tag></div></div>
          <div class="manage-owner"><span>上传者</span><strong>{{ picture.createdUser.username }}</strong><small>{{ picture.createtime }}</small></div>
          <div class="manage-action-cell">
            <a-button type="link" @click="router.push(`/prototype/gallery/detail/${picture.id}`)">详情</a-button>
            <a-button v-if="isAdmin && picture.pictureCheck === 0" type="link" @click="singleAction(picture, 'pass')">通过</a-button>
            <a-button v-if="isAdmin && picture.pictureCheck === 0" type="link" danger @click="singleAction(picture, 'refuse')">拒绝</a-button>
            <a-button v-if="!isAdmin && picture.pictureCheck === 2" type="link" @click="reupload(picture)">重新上传</a-button>
            <a-button type="link" danger @click="deletePicture(picture)">删除</a-button>
          </div>
        </article>
      </div>
    </section>

    <a-modal v-model:open="rejectOpen" title="填写拒绝原因" ok-text="提交拒绝" cancel-text="取消" @ok="confirmReject">
      <a-form layout="vertical" class="proto-form"><a-form-item label="checkMessage"><a-textarea v-model:value="rejectReason" :rows="4" placeholder="请输入审核拒绝原因" /></a-form-item></a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { pictureStatusText, prototypePictures, type PrototypePicture } from '../prototypeData'

const router = useRouter()
const isAdmin = ref(false)
const searchText = ref('')
const status = ref<'all' | 0 | 1 | 2>('all')
const spaceScope = ref('public')
const selectedIds = ref<number[]>([])
const rejectOpen = ref(false)
const rejectReason = ref('')
const pendingPicture = ref<PrototypePicture | null>(null)

const filteredPictures = computed(() => prototypePictures.filter((picture) => {
  const matchText = !searchText.value || `${picture.name}${picture.introduction}`.includes(searchText.value)
  const matchStatus = status.value === 'all' || picture.pictureCheck === status.value
  const matchSpace = spaceScope.value === 'public' ? picture.spaceId === 0 : spaceScope.value === 'private' ? picture.spaceId === 27 : picture.spaceId > 0
  return matchText && matchStatus && matchSpace
}))
const allSelected = computed(() => filteredPictures.value.length > 0 && filteredPictures.value.every((picture) => selectedIds.value.includes(picture.id)))

function statusClass(statusValue: number) { return statusValue === 1 ? 'pass' : statusValue === 2 ? 'refuse' : 'wait' }
function toggleOne(id: number, checked: boolean) { selectedIds.value = checked ? [...new Set([...selectedIds.value, id])] : selectedIds.value.filter((item) => item !== id) }
function toggleAll(event: any) { selectedIds.value = event.target.checked ? filteredPictures.value.map((picture) => picture.id) : [] }
function reset() { searchText.value = ''; status.value = 'all'; spaceScope.value = 'public'; selectedIds.value = [] }
function singleAction(picture: PrototypePicture, action: 'pass' | 'refuse') { if (action === 'refuse') { pendingPicture.value = picture; rejectOpen.value = true } else message.success(`原型演示：adminCheckPicture #${picture.id} / checkResult=1`) }
function confirmReject() { message.warning(`原型演示：adminCheckPicture #${pendingPicture.value?.id} / checkResult=2 / checkMessage=${rejectReason.value || '未填写'}`); rejectOpen.value = false; rejectReason.value = '' }
function batchAction(action: 'pass' | 'refuse') { if (action === 'refuse') { pendingPicture.value = null; rejectOpen.value = true } else message.success(`原型演示：adminCheckPictureBatch / picIds=${selectedIds.value.join(',')}`) }
function reupload(picture: PrototypePicture) { message.info(`原型演示：重传 #${picture.id}，对应 uploadPic 的更新流程`) }
function deletePicture(picture: PrototypePicture) { message.warning(`原型演示：deletePicture #${picture.id}`) }
</script>

<style scoped>
.role-switch { display: flex; align-items: center; gap: 11px; padding: 12px 14px; background: var(--proto-ink); color: var(--proto-paper); font-family: 'DM Mono', monospace; font-size: 10px; }
.role-switch :deep(.ant-switch-checked) { background: var(--proto-acid); }
.role-switch :deep(.ant-switch-checked .ant-switch-handle::before) { background: var(--proto-ink); }
.manage-toolbar { padding: 0; overflow: hidden; }
.manage-toolbar-line { padding: 17px; display: flex; gap: 10px; flex-wrap: wrap; align-items: center; border-bottom: 1px solid var(--proto-line); }
.manage-toolbar-line .ant-input { width: min(280px, 100%); }
.manage-toolbar-foot { padding: 14px 17px; display: flex; align-items: center; justify-content: space-between; gap: 12px; color: var(--proto-muted); font-family: 'DM Mono', monospace; font-size: 10px; }
.manage-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.manage-table { padding-bottom: 30px; }
.manage-table-head { display: flex; align-items: center; justify-content: space-between; padding-bottom: 13px; color: var(--proto-muted); font-size: 11px; }
.manage-list { border-top: 2px solid var(--proto-ink); }
.manage-row { min-height: 130px; padding: 12px 0; display: grid; grid-template-columns: 30px 150px minmax(200px, 1fr) 140px 210px; gap: 14px; align-items: center; border-bottom: 1px solid var(--proto-line); }
.manage-thumb { height: 102px; cursor: pointer; }
.manage-title-line { display: flex; align-items: center; gap: 9px; }
.manage-main strong { font-size: 16px; letter-spacing: -.04em; }
.manage-main p { margin: 8px 0 7px; color: var(--proto-muted); font-size: 11px; line-height: 1.5; }
.manage-tags { line-height: 1.3; }
.manage-owner span, .manage-owner strong, .manage-owner small { display: block; }
.manage-owner span { color: var(--proto-muted); font-size: 10px; }
.manage-owner strong { margin-top: 6px; font-size: 12px; }
.manage-owner small { margin-top: 7px; color: var(--proto-muted); font-family: 'DM Mono', monospace; font-size: 9px; }
.manage-action-cell { display: flex; align-items: center; justify-content: flex-end; flex-wrap: wrap; gap: 0; }
.manage-action-cell :deep(.ant-btn-link) { padding-inline: 5px; font-size: 11px; }
@media (max-width: 1050px) { .manage-row { grid-template-columns: 30px 120px minmax(170px, 1fr) 115px; } .manage-action-cell { grid-column: 3 / -1; justify-content: flex-start; } }
@media (max-width: 680px) { .manage-toolbar-foot { align-items: flex-start; flex-direction: column; } .manage-row { grid-template-columns: 25px 105px 1fr; gap: 9px; } .manage-owner, .manage-action-cell { grid-column: 3; justify-content: flex-start; } .manage-thumb { height: 82px; } }
</style>
