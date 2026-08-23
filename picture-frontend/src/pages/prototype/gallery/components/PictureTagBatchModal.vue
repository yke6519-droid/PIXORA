<template>
  <a-modal
    :open="open"
    title="批量管理标签"
    :footer="null"
    destroy-on-close
    @cancel="close"
  >
    <p class="tag-batch-hint">已选择 {{ pictureIds.length }} 张图片。每张图片最多绑定 3 个标签。</p>

    <a-select
      v-model:value="selectedTagIds"
      class="tag-batch-select"
      mode="multiple"
      :options="tagOptions"
      :loading="loading"
      :max-tag-count="3"
      allow-clear
      placeholder="选择要操作的标签"
    />

    <a-alert
      v-if="errorMessage"
      class="tag-batch-error"
      type="error"
      show-icon
      :message="errorMessage"
    />

    <div class="tag-batch-actions">
      <a-button :loading="loading" :disabled="!selectedTagIds.length" @click="submit('remove')">
        从图片移除
      </a-button>
      <a-button
        type="primary"
        :loading="loading"
        :disabled="!selectedTagIds.length"
        @click="submit('add')"
      >
        添加到图片
      </a-button>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { addPictureTags, removePictureTags } from '../../../../api/pictureTagController'

const props = defineProps<{
  open: boolean
  spaceId: number | string
  pictureIds: string[]
  tags: API.Tag[]
}>()

const emit = defineEmits<{
  (event: 'update:open', value: boolean): void
  (event: 'changed'): void
}>()

type Action = 'add' | 'remove'

const selectedTagIds = ref<string[]>([])
const loading = ref(false)
const errorMessage = ref('')
const tagOptions = computed(() => props.tags.map((tag) => ({
  label: tag.tagName,
  value: String(tag.id),
})))

async function submit(action: Action) {
  if (!props.pictureIds.length || !selectedTagIds.value.length) return

  loading.value = true
  errorMessage.value = ''
  try {
    const requestBody: API.PictureTagBatchRequest = {
      spaceId: props.spaceId,
      pictureIds: [...props.pictureIds],
      tagIds: [...selectedTagIds.value],
    }
    const res = action === 'add'
      ? await addPictureTags(requestBody)
      : await removePictureTags(requestBody)
    if (res.data?.code !== 200) {
      throw new Error(res.data?.message || '标签操作失败')
    }

    message.success(action === 'add' ? '标签已添加' : '标签已移除')
    emit('changed')
    close()
  } catch (error: any) {
    errorMessage.value = error?.response?.data?.message || error?.message || '标签操作失败'
  } finally {
    loading.value = false
  }
}

function close() {
  emit('update:open', false)
}

watch(
  () => props.open,
  (open) => {
    if (open) {
      selectedTagIds.value = []
      errorMessage.value = ''
    }
  },
)
</script>

<style scoped>
.tag-batch-hint { margin: 0 0 12px; color: var(--proto-muted); }
.tag-batch-select { width: 100%; }
.tag-batch-error { margin-top: 12px; }
.tag-batch-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 18px; }
</style>
