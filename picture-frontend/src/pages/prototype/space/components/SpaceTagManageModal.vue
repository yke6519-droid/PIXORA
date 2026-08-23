<template>
  <a-modal
    :open="open"
    :title="adminMode ? '空间标签审核' : '空间标签管理'"
    :confirm-loading="loading"
    :footer="null"
    destroy-on-close
    @cancel="close"
  >
    <div class="tag-manage-summary">
      <span>{{ adminMode ? '标签总数' : '可用标签数量' }}</span>
      <strong>{{ tags.length }} / {{ maxTagCount }}</strong>
    </div>

    <div v-if="!adminMode" class="tag-create-row">
      <a-input
        v-model:value="newTagName"
        :maxlength="32"
        show-count
        placeholder="输入新标签名称"
        @press-enter="create"
      />
      <a-button type="primary" :loading="loading" @click="create">新增</a-button>
    </div>

    <a-alert
      v-if="errorMessage"
      class="tag-manage-error"
      type="error"
      show-icon
      :message="errorMessage"
    />

    <a-empty v-if="!loading && !tags.length" description="还没有创建标签" />

    <div v-else class="tag-manage-list">
      <div v-for="tag in tags" :key="String(tag.id)" class="tag-manage-item">
        <template v-if="editingId === String(tag.id)">
          <a-input
            v-model:value="editingName"
            :maxlength="32"
            @press-enter="saveRename(tag)"
          />
          <a-space>
            <a-button type="link" size="small" :loading="loading" @click="saveRename(tag)">保存</a-button>
            <a-button type="link" size="small" @click="cancelRename">取消</a-button>
          </a-space>
        </template>

        <template v-else>
          <div class="tag-manage-name-wrap">
            <span class="tag-manage-name">{{ tag.tagName }}</span>
            <a-tag v-if="adminMode" :class="tag.status === 1 ? 'tag-status-active' : 'tag-status-disabled'">
              {{ tag.status === 1 ? '正常' : '已停用' }}
            </a-tag>
          </div>
          <a-space>
            <template v-if="adminMode">
              <a-popconfirm
                :title="tag.status === 1 ? '停用这个标签？' : '恢复这个标签？'"
                :description="tag.status === 1 ? '停用后不能绑定新图片，历史关联会保留。' : '恢复前会检查图片最多 3 个有效标签的限制。'"
                :ok-text="tag.status === 1 ? '停用' : '恢复'"
                cancel-text="取消"
                @confirm="toggleStatus(tag)"
              >
                <a-button type="link" size="small" :danger="tag.status === 1">
                  {{ tag.status === 1 ? '停用' : '恢复' }}
                </a-button>
              </a-popconfirm>
            </template>
            <template v-else>
              <a-button type="link" size="small" @click="startRename(tag)">改名</a-button>
              <a-popconfirm
                title="删除这个标签？"
                description="删除后会自动解除它与所有图片的关联。"
                ok-text="删除"
                cancel-text="取消"
                @confirm="remove(tag)"
              >
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </template>
          </a-space>
        </template>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import {
  listTag,
  listManageTag,
  createTag,
  renameTag,
  deleteTag,
  disableTag,
  restoreTag,
} from '../../../../api/tagController'

const props = defineProps<{
  open: boolean
  spaceId: number | string
  maxTagCount: number
  adminMode?: boolean
}>()

const emit = defineEmits<{
  (event: 'update:open', value: boolean): void
  (event: 'changed'): void
}>()

const tags = ref<API.Tag[]>([])
const newTagName = ref('')
const editingId = ref('')
const editingName = ref('')
const loading = ref(false)
const errorMessage = ref('')

async function loadTags() {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = props.adminMode
      ? await listManageTag({ spaceId: props.spaceId })
      : await listTag({ spaceId: props.spaceId })
    if (res.data?.code !== 200) {
      throw new Error(res.data?.message || '标签加载失败')
    }
    tags.value = res.data.data || []
  } catch (error: any) {
    errorMessage.value = error?.response?.data?.message || error?.message || '标签加载失败'
  } finally {
    loading.value = false
  }
}

async function create() {
  const tagName = newTagName.value.trim()
  if (!tagName) {
    errorMessage.value = '请输入标签名称'
    return
  }
  if (tags.value.length >= props.maxTagCount) {
    errorMessage.value = `当前空间最多只能创建 ${props.maxTagCount} 个标签`
    return
  }

  loading.value = true
  errorMessage.value = ''
  try {
    const res = await createTag({ spaceId: props.spaceId, tagName })
    if (res.data?.code !== 200) {
      throw new Error(res.data?.message || '标签创建失败')
    }
    newTagName.value = ''
    await loadTags()
    emit('changed')
  } catch (error: any) {
    errorMessage.value = error?.response?.data?.message || error?.message || '标签创建失败'
    loading.value = false
  }
}

function startRename(tag: API.Tag) {
  editingId.value = String(tag.id)
  editingName.value = tag.tagName || ''
  errorMessage.value = ''
}

function cancelRename() {
  editingId.value = ''
  editingName.value = ''
}

async function saveRename(tag: API.Tag) {
  const tagName = editingName.value.trim()
  if (!tagName || tag.id === undefined) {
    errorMessage.value = '请输入标签名称'
    return
  }

  loading.value = true
  errorMessage.value = ''
  try {
    const res = await renameTag({ tagId: tag.id, tagName })
    if (res.data?.code !== 200) {
      throw new Error(res.data?.message || '标签改名失败')
    }
    cancelRename()
    await loadTags()
    emit('changed')
  } catch (error: any) {
    errorMessage.value = error?.response?.data?.message || error?.message || '标签改名失败'
    loading.value = false
  }
}

async function remove(tag: API.Tag) {
  if (tag.id === undefined) return

  loading.value = true
  errorMessage.value = ''
  try {
    const res = await deleteTag({ tagId: tag.id })
    if (res.data?.code !== 200) {
      throw new Error(res.data?.message || '标签删除失败')
    }
    await loadTags()
    emit('changed')
  } catch (error: any) {
    errorMessage.value = error?.response?.data?.message || error?.message || '标签删除失败'
    loading.value = false
  }
}

async function toggleStatus(tag: API.Tag) {
  if (tag.id === undefined) return

  loading.value = true
  errorMessage.value = ''
  try {
    const request = tag.status === 1 ? disableTag : restoreTag
    const res = await request({ tagId: tag.id })
    if (res.data?.code !== 200 || res.data.data === false) {
      throw new Error(res.data?.message || '标签状态更新失败')
    }
    await loadTags()
    emit('changed')
  } catch (error: any) {
    errorMessage.value = error?.response?.data?.message || error?.message || '标签状态更新失败'
    loading.value = false
  }
}

function close() {
  emit('update:open', false)
}

watch(
  () => [props.open, props.spaceId, props.adminMode] as const,
  ([open]) => {
    if (open) {
      newTagName.value = ''
      editingId.value = ''
      editingName.value = ''
      void loadTags()
    }
  },
)
</script>

<style scoped>
.tag-manage-summary { display: flex; justify-content: space-between; margin-bottom: 14px; color: var(--proto-muted); }
.tag-manage-summary strong { color: var(--proto-ink); }
.tag-create-row { display: flex; gap: 8px; margin-bottom: 14px; }
.tag-manage-error { margin-bottom: 14px; }
.tag-manage-list { display: grid; gap: 8px; max-height: 360px; overflow-y: auto; }
.tag-manage-item { display: flex; align-items: center; justify-content: space-between; gap: 10px; min-height: 38px; padding: 6px 8px; border: 1px solid var(--proto-line); border-radius: 6px; }
.tag-manage-item > .ant-input { flex: 1; }
.tag-manage-name-wrap { min-width: 0; display: flex; align-items: center; gap: 8px; }
.tag-manage-name { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--proto-ink); }
.tag-status-active.ant-tag, .tag-status-disabled.ant-tag { margin: 0; font-size: 10px; }
.tag-status-disabled.ant-tag { color: var(--proto-orange); }
</style>
