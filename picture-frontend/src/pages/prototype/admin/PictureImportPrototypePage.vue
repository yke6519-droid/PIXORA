<template>
  <div class="import-prototype">
    <section class="proto-page-head">
      <div>
        <span class="proto-eyebrow">批量抓图 / adminFetchPictureBatch</span>
        <h1 class="proto-title">让管理员的<br />批量动作更有节奏。</h1>
        <p class="proto-copy">
          后端从 Bing 图片结果中抓取图片并逐张上传。任务完成后返回本次成功落库的图片列表；数量上限为 20。
        </p>
      </div>
      <div class="import-limit"><span>单次上限</span><strong>20</strong><small>count</small></div>
    </section>

    <a-spin v-if="authChecking" class="import-auth-loading" tip="正在确认管理员权限..." />

    <a-result
      v-else-if="!authorized"
      status="403"
      title="暂时无法使用批量抓图"
      :sub-title="accessError || '只有管理员可以执行批量抓图。'"
    >
      <template #extra>
        <a-button class="proto-button ghost-button" @click="ensureAdmin">重新检查权限</a-button>
      </template>
    </a-result>

    <template v-else>
      <a-alert
        v-if="loadError"
        class="import-alert"
        type="error"
        show-icon
        :message="loadError"
        closable
        @close="loadError = ''"
      />

      <section class="import-layout proto-section">
        <div class="import-form-card proto-surface proto-rounded">
          <div class="form-card-heading">
            <span class="proto-eyebrow">batch request</span>
            <h2>配置抓取任务</h2>
          </div>
          <!-- 绑定表单模型后，Ant Design Vue 才会在点击提交时触发 finish 回调。 -->
          <a-form :model="form" layout="vertical" class="proto-form" @finish="submitImport">
            <a-form-item label="搜索词 searchText" required>
              <a-input v-model:value="form.searchText" placeholder="例如：minimal architecture" />
            </a-form-item>
            <div class="import-two-col">
              <a-form-item label="数量 count" required>
                <a-input-number v-model:value="form.count" :min="1" :max="20" style="width: 100%" />
              </a-form-item>
              <a-form-item label="默认名称 name">
                <a-input v-model:value="form.name" placeholder="为空时使用 searchText" />
              </a-form-item>
            </div>
            <a-form-item label="分类 category">
              <a-select
                v-model:value="form.category"
                allow-clear
                :loading="optionsLoading"
                placeholder="选择分类（可不填）"
                style="width: 100%"
              >
                <a-select-option v-for="category in categories" :key="category" :value="category">
                  {{ category }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="标签 tags">
              <div class="import-tags">
                <a-checkable-tag
                  v-for="tag in tags"
                  :key="tag"
                  :checked="form.tags?.includes(tag)"
                  @change="(checked: boolean) => toggleTag(tag, checked)"
                >
                  {{ tag }}
                </a-checkable-tag>
                <span v-if="!tags.length" class="import-no-options">暂无可选标签</span>
              </div>
            </a-form-item>
            <div class="import-contract">
              <span>后端实际写入</span>
              <strong>name / category / tags / introduction</strong>
              <small>introduction 由后端固定为「搜索词 + 相关图片」</small>
            </div>
            <a-button
              html-type="submit"
              class="proto-button acid-button import-submit"
              type="primary"
              :loading="submitting"
            >
              开始批量抓取
            </a-button>
          </a-form>
        </div>

        <div class="import-result-panel">
          <div class="import-preview-head">
            <span class="proto-eyebrow">response / real</span>
            <h2>任务结果</h2>
          </div>
          <div class="import-result-main proto-surface proto-rounded">
            <span class="import-result-kicker">最近一次执行</span>
            <strong>{{ importedMessage }}</strong>
            <small v-if="lastExecutedAt">{{ lastExecutedAt }} · 关键词：{{ lastSearchText }}</small>
            <small v-else>尚未提交抓取任务</small>
            <div v-if="hasImportResult" class="import-result-stats" aria-label="批量抓图结果统计">
              <div class="import-result-stat">
                <span>本次处理</span>
                <strong>{{ targetCount }}</strong>
              </div>
              <div class="import-result-stat">
                <span>成功入库</span>
                <strong>{{ successCount }}</strong>
              </div>
              <div class="import-result-stat">
                <span>未成功</span>
                <strong>{{ pendingCount }}</strong>
              </div>
            </div>
          </div>
          <section v-if="importedPictures.length" class="import-picture-results proto-surface proto-rounded">
            <div class="import-picture-results-head">
              <div>
                <span class="proto-eyebrow">本次成功图片</span>
                <h3>成功落库的图片</h3>
              </div>
              <strong>{{ successCount }} 张</strong>
            </div>
            <div class="import-picture-grid">
              <article
                v-for="picture in importedPictures"
                :key="String(picture.id)"
                class="import-picture-card"
                role="button"
                tabindex="0"
                @click="openPictureDetail(picture.id)"
                @keydown.enter.prevent="openPictureDetail(picture.id)"
                @keydown.space.prevent="openPictureDetail(picture.id)"
              >
                <div class="import-picture-media">
                  <img
                    v-if="picture.thumbnailUrl || picture.url"
                    :src="picture.thumbnailUrl || picture.url"
                    :alt="picture.name || '批量抓取图片'"
                  />
                  <span v-else>暂无预览</span>
                </div>
                <div class="import-picture-meta">
                  <strong>{{ picture.name || '未命名图片' }}</strong>
                  <small>
                    {{ picture.picwidth || '--' }} × {{ picture.picheight || '--' }}
                    · {{ formatPictureSize(picture.picsize) }}
                  </small>
                </div>
              </article>
            </div>
          </section>
          <a-empty
            v-else-if="hasImportResult"
            class="import-picture-empty proto-surface proto-rounded"
            description="本次没有返回可展示的图片"
          />
          <div class="import-boundary">
            <span class="proto-eyebrow">结果说明</span>
            <p>接口会同时返回本次处理数量、成功入库数量和图片详情；任务完成后仍可到图片管理继续编辑和审核。</p>
            <a-button class="proto-button ghost-button" @click="openPictureManage">查看图片管理</a-button>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { adminFetchPictureBatchUsingPost } from '../../../api/pictureController'
import { listPictureCategoryUsingGet } from '../../../api/pictureController'
import { getCurrentUserUsingGet } from '../../../api/userController'
import { useLoginUserStore } from '../../../stores/useLoginUserStore'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const authChecking = ref(true)
const authorized = ref(false)
const accessError = ref('')
const optionsLoading = ref(false)
const submitting = ref(false)
const loadError = ref('')
const categories = ref<string[]>([])
const tags = ref<string[]>([])
const importedMessage = ref('等待执行')
const importedPictures = ref<API.PictureVO[]>([])
const targetCount = ref(0)
const successCount = ref(0)
const hasImportResult = ref(false)
const lastSearchText = ref('')
const lastExecutedAt = ref('')
const form = reactive<API.PictureUploadByBatchRequest>({
  searchText: 'minimal architecture',
  count: 6,
  name: '',
  category: undefined,
  tags: [],
})
const pendingCount = computed(() => Math.max(targetCount.value - successCount.value, 0))

async function ensureAdmin() {
  authChecking.value = true
  accessError.value = ''
  try {
    const res = await getCurrentUserUsingGet()
    if (res.data?.code === 40100 || !res.data?.data) {
      loginUserStore.clearLoginUser()
      await router.replace({ path: '/prototype/user/login', query: { redirect: route.fullPath } })
      return false
    }
    if (res.data.code !== 200) {
      accessError.value = res.data.message || '当前用户信息加载失败'
      authorized.value = false
      return false
    }
    loginUserStore.setLoginUser(res.data.data)
    if (res.data.data.userLevel !== 'admin') {
      accessError.value = '当前账号不是管理员，无法调用批量抓图接口。'
      authorized.value = false
      return false
    }
    authorized.value = true
    await loadOptions()
    return true
  } catch (error: any) {
    authorized.value = false
    accessError.value = error?.response?.data?.message || error?.message || '管理员权限检查失败'
    return false
  } finally {
    authChecking.value = false
  }
}

async function loadOptions() {
  optionsLoading.value = true
  try {
    const res = await listPictureCategoryUsingGet()
    if (res.data?.code === 200) {
      categories.value = res.data.data?.categorys || []
      tags.value = res.data.data?.tags || []
    } else {
      loadError.value = res.data?.message || '分类和标签加载失败，仍可提交无分类任务'
    }
  } catch (error: any) {
    loadError.value = error?.response?.data?.message || error?.message || '分类和标签加载失败，仍可提交无分类任务'
  } finally {
    optionsLoading.value = false
  }
}

function toggleTag(tag: string, checked: boolean) {
  const currentTags = form.tags || []
  form.tags = checked
    ? [...new Set([...currentTags, tag])]
    : currentTags.filter((item) => item !== tag)
}

/** 将后端返回的字节数转换为适合列表展示的文件大小。 */
function formatPictureSize(size?: number) {
  if (!size || size <= 0) return '大小未知'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

/** 使用字符串保留 Long 图片 ID 的完整精度，再进入原型详情页。 */
function openPictureDetail(id?: number | string) {
  const normalizedId = String(id || '').trim()
  if (!normalizedId) return
  void router.push(`/prototype/gallery/detail/${encodeURIComponent(normalizedId)}`)
}

async function submitImport() {
  loadError.value = ''
  const searchText = form.searchText?.trim()
  const count = Number(form.count)
  if (!searchText) {
    message.warning('请输入搜索词')
    return
  }
  if (!Number.isInteger(count) || count < 1 || count > 20) {
    message.warning('抓取数量必须是 1 到 20 之间的整数')
    return
  }

  submitting.value = true
  // 新任务开始时清除上一次结果，避免用户误把旧图片当成本次返回结果。
  importedPictures.value = []
  targetCount.value = 0
  successCount.value = 0
  hasImportResult.value = false
  importedMessage.value = '正在执行抓取任务'
  try {
    const res = await adminFetchPictureBatchUsingPost({
      searchText,
      count,
      name: form.name?.trim() || undefined,
      category: form.category || undefined,
      tags: form.tags?.length ? [...form.tags] : undefined,
    })
    if (res.data?.code !== 200) throw new Error(res.data?.message || '批量抓图失败')
    const result = res.data.data
    const pictures = (result?.pictureList || []).filter(
      (picture): picture is API.PictureVO => Boolean(picture),
    )
    targetCount.value = Number(result?.targetCount || 0)
    successCount.value = Number(result?.successCount || 0)
    importedPictures.value = pictures
    hasImportResult.value = true
    importedMessage.value = successCount.value
      ? `成功入库 ${successCount.value} / 处理 ${targetCount.value} 张`
      : '本次没有成功落库的图片'
    lastSearchText.value = searchText
    lastExecutedAt.value = new Date().toLocaleString('zh-CN', { hour12: false })
    if (successCount.value > 0) {
      message.success(`批量抓图完成，成功入库 ${successCount.value} 张`)
    } else {
      message.warning('批量抓图完成，但没有图片成功落库')
    }
  } catch (error: any) {
    loadError.value = error?.response?.data?.message || error?.message || '批量抓图失败，请稍后重试'
    message.error(loadError.value)
  } finally {
    submitting.value = false
  }
}

function openPictureManage() {
  void router.push('/prototype/gallery/manage')
}

onMounted(() => {
  void ensureAdmin()
})
</script>

<style scoped>
.import-auth-loading { display: block; min-height: 180px; padding-top: 70px; text-align: center; }
.import-alert { margin-bottom: 18px; }
.import-prototype > .proto-page-head { padding-top: 0; gap: 14px; }
.import-prototype > .proto-page-head .proto-title { margin-top: 7px; font-size: clamp(30px, 3.5vw, 50px); }
.import-prototype > .proto-page-head .proto-copy { max-width: 560px; font-size: 11px; line-height: 1.45; }
.import-limit { min-width: 130px; padding: 15px; background: var(--proto-acid); }
.import-limit span, .import-limit strong, .import-limit small { display: block; }
.import-limit span { font-family: 'DM Mono', monospace; font-size: 10px; }
.import-limit strong { margin-top: 14px; font-size: 49px; line-height: .8; letter-spacing: -.1em; }
.import-limit small { margin-top: 11px; font-family: 'DM Mono', monospace; font-size: 10px; opacity: .55; }
.import-layout.proto-section { display: grid; grid-template-columns: minmax(0, .85fr) minmax(0, 1.15fr); gap: var(--prototype-layout-gap); padding-top: 10px; }
.import-form-card { padding: clamp(14px, 2.1vw, 23px); }
.import-form-card :deep(.ant-form-item) { margin-bottom: 11px; }
.form-card-heading { margin-bottom: 14px; }
.form-card-heading h2, .import-preview-head h2 { margin: 6px 0 10px; font-size: 27px; letter-spacing: -.07em; }
.import-two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 13px; }
.import-tags :deep(.ant-tag-checkable) { padding: 4px 9px; border-radius: 3px; font-size: 11px; }
.import-tags :deep(.ant-tag-checkable-checked) { background: var(--proto-acid); color: var(--proto-ink); }
.import-no-options { color: var(--proto-muted); font-size: 11px; }
.import-contract { display: flex; flex-direction: column; gap: 4px; margin: 12px 0; padding: 10px; border-left: 2px solid var(--proto-orange); background: rgba(255,137,106,.12); color: var(--proto-muted); font-family: 'DM Mono', monospace; font-size: 9px; }
.import-contract strong { color: var(--proto-ink); font-weight: 500; }
.import-contract small { line-height: 1.5; }
.import-submit { width: 100%; }
.import-result-panel { min-width: 0; padding: 2px 0; }
.import-preview-head h2 { margin-bottom: 14px; }
.import-result-main { min-height: 220px; padding: 25px; display: flex; flex-direction: column; justify-content: center; }
.import-result-kicker { color: var(--proto-orange); font-family: 'DM Mono', monospace; font-size: 10px; }
.import-result-main > strong { margin-top: 18px; font-size: clamp(22px, 3vw, 38px); line-height: 1.1; letter-spacing: -.06em; }
.import-result-main small { margin-top: 16px; color: var(--proto-muted); font-size: 11px; line-height: 1.6; }
.import-result-stats { margin-top: 22px; padding-top: 13px; display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; border-top: 1px solid var(--proto-line); }
.import-result-stat { min-width: 0; }
.import-result-stat span { display: block; color: var(--proto-muted); font-size: 10px; }
.import-result-stat strong { display: block; margin-top: 4px; color: var(--proto-ink); font-size: 22px; line-height: 1; }
.import-picture-results { margin-top: 12px; padding: 16px; }
.import-picture-results-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 12px; margin-bottom: 14px; }
.import-picture-results-head h3 { margin: 6px 0 0; font-size: 20px; letter-spacing: -.05em; }
.import-picture-results-head > strong { color: var(--proto-orange); font-family: 'DM Mono', monospace; font-size: 13px; font-weight: 500; white-space: nowrap; }
.import-picture-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; max-height: 360px; overflow: auto; }
.import-picture-card { min-width: 0; border: 1px solid var(--proto-line); background: rgba(255,255,255,.55); cursor: pointer; transition: border-color 160ms ease, background-color 160ms ease; }
.import-picture-card:hover { border-color: var(--proto-orange); background: rgba(255,255,255,.85); }
.import-picture-card:focus-visible { outline: 2px solid var(--proto-orange); outline-offset: 3px; }
.import-picture-media { aspect-ratio: 16 / 10; display: grid; place-items: center; overflow: hidden; background: rgba(20,24,27,.08); color: var(--proto-muted); font-size: 10px; }
.import-picture-media img { width: 100%; height: 100%; display: block; object-fit: cover; }
.import-picture-meta { min-width: 0; padding: 9px 10px 10px; }
.import-picture-meta strong, .import-picture-meta small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.import-picture-meta strong { color: var(--proto-ink); font-size: 12px; font-weight: 600; }
.import-picture-meta small { margin-top: 5px; color: var(--proto-muted); font-family: 'DM Mono', monospace; font-size: 9px; }
.import-picture-empty { margin: 12px 0 0; padding: 22px 16px; }
.import-boundary { margin-top: 12px; padding: 18px; border: 1px solid var(--proto-line); background: rgba(255,255,255,.45); }
.import-boundary p { max-width: 500px; margin: 10px 0 17px; color: var(--proto-muted); font-size: 11px; line-height: 1.7; }
@media (max-width: 980px) { .import-layout.proto-section { grid-template-columns: 1fr; } }
@media (max-width: 520px) { .import-two-col, .import-picture-grid, .import-result-stats { grid-template-columns: 1fr; gap: 0; } .import-picture-card + .import-picture-card, .import-result-stat + .import-result-stat { margin-top: 10px; } }
</style>
