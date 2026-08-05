<template>
  <a-modal
    v-model:open="visible"
    title="批量拉取图片"
    width="500px"
    :confirm-loading="loading"
    @ok="handleFetch"
    @cancel="handleCancel"
    ok-text="开始拉取"
    cancel-text="取消"
  >
    <a-form
      :model="formData"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 18 }"
      class="batch-form"
    >
      <a-form-item label="搜索关键词" required>
        <a-input
          v-model:value="formData.searchText"
          placeholder="请输入搜索关键词（如：风景、动物）"
          :maxLength="50"
          show-count
          allow-clear
        />
      </a-form-item>

      <a-form-item label="图片名称前缀">
        <a-input
          v-model:value="formData.name"
          placeholder="请输入图片名称前缀（可选，默认使用搜索关键词）"
          :maxLength="30"
          show-count
          allow-clear
        />
        <p class="form-tip">拉取的图片将以「前缀_序号」命名，如：风景_01、风景_02</p>
      </a-form-item>

      <a-form-item label="拉取数量" required>
        <a-input-number
          v-model:value="formData.count"
          :min="1"
          :max="20"
          placeholder="请输入拉取数量"
          style="width: 100%"
        />
        <p class="form-tip">最多拉取 20 张图片，通过 Bing 图片搜索获取</p>
      </a-form-item>

      <a-form-item :wrapper-col="{ offset: 6, span: 18 }">
        <a-alert
          message="拉取的图片将自动审核通过并添加到图库中"
          type="info"
          show-icon
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { adminFetchPictureBatchUsingPost } from '../api/pictureController'

// 定义 Props
interface Props {
  open: boolean
}

const props = withDefaults(defineProps<Props>(), {
  open: false
})

// 定义 Emits
const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'success'): void
}>()

// 弹窗显示状态
const visible = ref(false)

// 加载状态
const loading = ref(false)

// 表单数据
const formData = ref({
  searchText: '',
  name: '',
  count: 10
})

// 监听 props.open 变化
watch(() => props.open, (newVal) => {
  visible.value = newVal
})

// 监听 visible 变化，同步给父组件
watch(visible, (newVal) => {
  emit('update:open', newVal)
})

/**
 * 开始拉取
 */
const handleFetch = async () => {
  if (!formData.value.searchText.trim()) {
    message.warning('请输入搜索关键词')
    return
  }

  if (!formData.value.count || formData.value.count < 1) {
    message.warning('拉取数量至少为 1')
    return
  }

  if (formData.value.count > 20) {
    message.warning('拉取数量最多为 20')
    return
  }

  loading.value = true
  try {
    const res = await adminFetchPictureBatchUsingPost({
      searchText: formData.value.searchText.trim(),
      name: formData.value.name?.trim() || undefined,
      count: formData.value.count
    })

    if (res.data.code === 200) {
      // 后端返回本次成功落库的图片列表，旧弹窗只反馈数量，父页面负责刷新图库。
      const pictures = res.data.data || []
      message.success(`批量拉取完成，成功 ${pictures.length} 张`)
      visible.value = false
      // 重置表单
      formData.value = { searchText: '', name: '', count: 10 }
      // 通知父组件刷新页面
      emit('success')
    } else {
      message.error('批量拉取失败：' + res.data.message)
    }
  } catch (error) {
    message.error('批量拉取失败，请重试')
  } finally {
    loading.value = false
  }
}

/**
 * 取消
 */
const handleCancel = () => {
  formData.value = { searchText: '', name: '', count: 10 }
}
</script>

<style scoped>
.batch-form {
  padding: 16px 0 0;
}

.form-tip {
  color: #999;
  font-size: 12px;
  margin-top: 4px;
  margin-bottom: 0;
}
</style>
