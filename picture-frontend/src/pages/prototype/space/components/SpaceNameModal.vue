<template>
  <a-modal
    :open="open"
    :title="mode === 'create' ? '创建私人空间' : '重命名空间'"
    :ok-text="mode === 'create' ? '创建空间' : '保存名称'"
    cancel-text="取消"
    :confirm-loading="submitting"
    :mask-closable="!submitting"
    @ok="submit"
    @cancel="emit('update:open', false)"
  >
    <a-form ref="formRef" :model="form" layout="vertical" class="proto-form space-name-form">
      <a-form-item
        label="空间名称"
        name="spaceName"
        :rules="[{ required: true, whitespace: true, message: '请输入空间名称' }]"
      >
        <a-input v-model:value="form.spaceName" placeholder="为私人空间命名" />
      </a-form-item>
      <a-alert
        v-if="mode === 'create'"
        type="info"
        show-icon
        message="每个用户只能创建一个私人空间，创建后仍可修改名称。"
      />
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import type { FormInstance } from 'ant-design-vue'

const props = defineProps<{
  open: boolean
  mode: 'create' | 'rename'
  initialName?: string
  submitting?: boolean
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  submit: [spaceName: string]
}>()

const formRef = ref<FormInstance>()
const form = reactive({ spaceName: '' })

async function submit() {
  try {
    await formRef.value?.validate()
    emit('submit', form.spaceName.trim())
  } catch {
    // 表单组件已经展示具体校验信息。
  }
}

watch(
  () => props.open,
  (isOpen) => {
    if (isOpen) {
      form.spaceName = props.initialName || ''
      formRef.value?.clearValidate()
    }
  },
)
</script>

<style scoped>
.space-name-form { padding-top: 8px; }
</style>
