<template>
  <div class="upload-prototype">
    <section class="proto-page-head">
      <div>
        <span class="proto-eyebrow">上传图片 / uploadPic</span>
        <h1 class="proto-title">把图片放进<br />正确的空间。</h1>
        <p class="proto-copy">当前原型完整保留上传接口字段：文件或 URL、名称、分类、标签、简介和 `spaceId`。普通用户上传后默认进入待审核。</p>
      </div>
      <div class="upload-contract"><span>普通用户</span><strong>审核状态</strong><b>待审核</b></div>
    </section>

    <section class="upload-layout proto-section">
      <div class="upload-dropzone proto-surface proto-rounded">
        <div class="upload-dropzone-mark">+</div>
        <h2>拖入一张图片</h2>
        <p>支持 JPG、PNG、WEBP，原型只模拟文件选择，不会上传到 COS。</p>
        <a-upload-dragger :before-upload="beforeUpload" :show-upload-list="false" accept=".jpg,.jpeg,.png,.webp">
          <p class="upload-drag-title">点击或拖拽文件到这里</p>
          <p class="upload-drag-copy">接口字段：file</p>
        </a-upload-dragger>
        <div class="upload-or"><span>或者</span></div>
        <a-input v-model:value="imageUrl" placeholder="粘贴图片 URL，接口字段：url" />
        <div v-if="fileReady || imageUrl" class="upload-ready"><i></i>{{ fileReady ? '已选择本地文件' : '已填写图片 URL' }}</div>
      </div>

      <div class="upload-form-card proto-surface proto-rounded">
        <div class="form-card-heading"><span class="proto-eyebrow">metadata</span><h2>补充图片信息</h2></div>
        <a-form layout="vertical" class="proto-form" @finish="submitUpload">
          <a-form-item label="图片名称" required><a-input v-model:value="form.name" placeholder="例如：Quiet forest" /></a-form-item>
          <div class="form-two-col">
            <a-form-item label="图片分类"><a-select v-model:value="form.category" style="width: 100%"><a-select-option v-for="item in categories.slice(1)" :key="item" :value="item">{{ item }}</a-select-option></a-select></a-form-item>
            <a-form-item label="上传到空间"><a-select v-model:value="form.spaceId" style="width: 100%"><a-select-option :value="0">公共图库</a-select-option><a-select-option :value="27">周野的私人空间</a-select-option><a-select-option :value="28">林默的私人空间</a-select-option></a-select></a-form-item>
          </div>
          <a-form-item label="标签 tags"><div class="upload-tags"><a-checkable-tag v-for="tag in tags" :key="tag" :checked="form.tags.includes(tag)" @change="(checked: boolean) => toggleTag(tag, checked)">{{ tag }}</a-checkable-tag></div></a-form-item>
          <a-form-item label="图片简介"><a-textarea v-model:value="form.introduction" :rows="4" placeholder="介绍图片内容或来源" /></a-form-item>
          <div class="upload-form-foot"><span>当前提交字段 {{ form.tags.length + 5 }} 项</span><a-button html-type="submit" class="proto-button acid-button" type="primary">提交图片</a-button></div>
        </a-form>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { prototypeCategories, prototypeTags } from '../prototypeData'

const router = useRouter()
const categories = prototypeCategories
const tags = prototypeTags.slice(0, 7)
const imageUrl = ref('')
const fileReady = ref(false)
const form = reactive({ name: 'New visual archive', category: '风景', spaceId: 0, tags: ['自然'], introduction: '一张等待进入图库的图片。' })

function beforeUpload() {
  fileReady.value = true
  return false
}

function toggleTag(tag: string, checked: boolean) {
  if (checked) form.tags.push(tag)
  else form.tags = form.tags.filter((item) => item !== tag)
}

function submitUpload() {
  message.success(form.spaceId === 0 ? '原型提交成功：普通用户图片将进入待审核' : '原型提交成功：已写入指定 spaceId')
  router.push('/prototype/gallery/manage')
}
</script>

<style scoped>
.upload-prototype { height: 100%; min-height: 0; display: flex; flex-direction: column; }
.upload-prototype > .proto-page-head { padding-top: 0; gap: 14px; }
.upload-prototype > .proto-page-head .proto-title { margin-top: 7px; font-size: clamp(30px, 3.5vw, 50px); }
.upload-prototype > .proto-page-head .proto-copy { max-width: 500px; font-size: 11px; line-height: 1.45; }
.upload-contract { min-width: 170px; padding: 15px 17px; background: var(--proto-ink); color: var(--proto-paper); }
.upload-contract span, .upload-contract strong, .upload-contract b { display: block; }
.upload-contract span { color: var(--proto-acid); font-family: 'DM Mono', monospace; font-size: 10px; }
.upload-contract strong { margin-top: 18px; font-size: 13px; }
.upload-contract b { margin-top: 5px; color: var(--proto-orange); font-size: 18px; }
.upload-layout.proto-section { flex: 1 1 auto; min-height: 0; padding-top: 10px; overflow: hidden; }
.upload-layout { display: grid; grid-template-columns: minmax(300px, .88fr) minmax(420px, 1.12fr); grid-template-rows: minmax(0, 1fr); gap: 12px; align-items: stretch; }
.upload-dropzone { height: 100%; min-height: 0; padding: clamp(12px, 1.9vw, 22px); display: flex; flex-direction: column; justify-content: center; background: var(--proto-ink); color: var(--proto-paper); }
.upload-dropzone-mark { width: 36px; height: 36px; display: grid; place-items: center; margin-bottom: 8px; background: var(--proto-acid); color: var(--proto-ink); font-size: 23px; line-height: 1; }
.upload-dropzone h2 { margin: 0; font-size: clamp(24px, 2.8vw, 36px); line-height: .95; letter-spacing: -.07em; }
.upload-dropzone > p { max-width: 330px; margin: 7px 0 8px; color: rgba(241,242,237,.62); font-size: 10px; line-height: 1.4; }
.upload-dropzone :deep(.ant-upload.ant-upload-drag) { border: 1px dashed rgba(241,242,237,.32); background: rgba(241,242,237,.05); border-radius: 4px; }
.upload-dropzone :deep(.ant-upload.ant-upload-drag:hover) { border-color: var(--proto-acid); }
.upload-dropzone :deep(.ant-upload.ant-upload-drag) { height: auto; min-height: 0; padding: 8px; }
.upload-dropzone :deep(.ant-upload-text) { color: var(--proto-paper); }
.upload-dropzone :deep(.ant-upload-hint) { color: rgba(241,242,237,.45); }
.upload-drag-title { margin: 0; color: var(--proto-paper); font-size: 12px; }
.upload-drag-copy { margin: 4px 0 0; color: rgba(241,242,237,.44); font-family: 'DM Mono', monospace; font-size: 9px; }
.upload-or { display: flex; align-items: center; gap: 10px; margin: 7px 0; color: rgba(241,242,237,.42); font-family: 'DM Mono', monospace; font-size: 9px; }
.upload-or::before, .upload-or::after { content: ''; height: 1px; flex: 1; background: rgba(241,242,237,.18); }
.upload-dropzone :deep(.ant-input) { border: 1px solid rgba(241,242,237,.23); background: rgba(241,242,237,.06); color: var(--proto-paper); border-radius: 4px; }
.upload-dropzone :deep(.ant-input::placeholder) { color: rgba(241,242,237,.4); }
.upload-ready { display: flex; align-items: center; gap: 7px; margin-top: 6px; color: var(--proto-acid); font-family: 'DM Mono', monospace; font-size: 9px; }
.upload-ready i { width: 7px; height: 7px; border-radius: 50%; background: var(--proto-acid); }
.upload-form-card { height: 100%; min-height: 0; display: flex; flex-direction: column; padding: clamp(14px, 2.1vw, 23px); }
.upload-form-card :deep(.ant-form-item) { margin-bottom: 7px; }
.upload-form-card :deep(textarea.ant-input) { height: 54px !important; min-height: 0; padding-block: 5px; line-height: 1.35; }
.form-card-heading { margin-bottom: 8px; }
.form-card-heading h2 { margin: 6px 0 0; font-size: 27px; letter-spacing: -.07em; }
.form-two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 9px; }
.upload-tags :deep(.ant-tag-checkable) { padding: 4px 9px; border-radius: 3px; font-size: 11px; }
.upload-tags :deep(.ant-tag-checkable-checked) { background: var(--proto-acid); color: var(--proto-ink); }
.upload-form-foot { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 7px; color: var(--proto-muted); font-family: 'DM Mono', monospace; font-size: 10px; }
@media (max-width: 850px) { .upload-prototype { height: auto; } .upload-layout.proto-section { flex: none; padding-top: 18px; overflow: visible; } .upload-layout { grid-template-columns: 1fr; } .upload-dropzone { height: auto; min-height: 430px; } .upload-form-card { height: auto; } }
@media (max-width: 500px) { .form-two-col { grid-template-columns: 1fr; gap: 0; } .upload-form-foot { align-items: flex-start; flex-direction: column; } }
</style>
