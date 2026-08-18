<template>
  <div class="import-prototype">
    <section class="import-page-heading">
      <div>
        <h1>批量抓图</h1>
        <a-tag class="import-role-tag">管理员功能</a-tag>
      </div>
      <div class="import-limit">
        <span>单次最多</span>
        <strong>20</strong>
        <small>张图片</small>
      </div>
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

      <section class="import-layout">
        <section class="import-form-card proto-rounded">
          <div class="import-section-heading">
            <h2>配置抓取任务</h2>
            <span class="import-section-note">填写搜索条件</span>
          </div>
          <!-- 绑定表单模型后，Ant Design Vue 才会在点击提交时触发 finish 回调。 -->
          <a-form :model="form" layout="vertical" class="proto-form" @finish="submitImport">
            <a-form-item label="搜索词" required>
              <a-input v-model:value="form.searchText" placeholder="例如：minimal architecture" />
            </a-form-item>
            <div class="import-two-col">
              <a-form-item label="抓取数量" required>
                <a-input-number v-model:value="form.count" :min="1" :max="20" style="width: 100%" />
              </a-form-item>
              <a-form-item label="图片名称（可选）">
                <a-input v-model:value="form.name" placeholder="为空时使用 searchText" />
              </a-form-item>
            </div>
            <a-form-item label="分类（可选）">
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
            <a-form-item label="标签（可选）">
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
            <div class="import-submit-row">
              <span>提交后，成功图片会显示在右侧</span>
              <a-button
                html-type="submit"
                class="proto-button acid-button import-submit"
                type="primary"
                :loading="submitting"
              >
                开始抓取
              </a-button>
            </div>
          </a-form>
        </section>

        <section class="import-result-panel">
          <div class="import-section-heading">
            <h2>任务结果</h2>
            <a-button class="proto-button ghost-button" @click="openPictureManage">图片管理</a-button>
          </div>
          <div class="import-result-main proto-surface proto-rounded">
            <div v-if="submitting" class="import-result-empty">
              <a-spin tip="正在抓取并上传图片..." />
            </div>
            <a-empty
              v-else-if="!hasImportResult"
              class="import-result-empty"
              description="提交任务后，结果会显示在这里"
            />
            <div v-else class="import-result-complete">
              <a-tag class="import-status-tag">执行完成</a-tag>
              <strong>{{ importedMessage }}</strong>
              <small>{{ lastExecutedAt }} · 关键词：{{ lastSearchText }}</small>
              <div class="import-result-stats" aria-label="批量抓图结果统计">
                <div class="import-result-stat">
                  <span>目标数量</span>
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
          </div>
          <section v-if="importedPictures.length" class="import-picture-results proto-surface proto-rounded">
            <div class="import-picture-results-head">
              <h3>已入库图片</h3>
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
        </section>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { adminFetchPictureBatch, listPictureCategory } from '../../../api/pictureController'
import { getCurrentUser } from '../../../api/userController'
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
    const res = await getCurrentUser()
    if (res.data?.code === 40100 || !res.data?.data) {
      loginUserStore.clearLoginUser()
      await router.replace({ path: '/user/login', query: { redirect: route.fullPath } })
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
    const res = await listPictureCategory()
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
function formatPictureSize(size?: number | string) {
  const normalizedSize = Number(size)
  if (!Number.isFinite(normalizedSize) || normalizedSize <= 0) return '大小未知'
  if (normalizedSize < 1024) return `${normalizedSize} B`
  if (normalizedSize < 1024 * 1024) return `${(normalizedSize / 1024).toFixed(1)} KB`
  return `${(normalizedSize / 1024 / 1024).toFixed(1)} MB`
}

/** 使用字符串保留 Long 图片 ID 的完整精度，再进入原型详情页。 */
function openPictureDetail(id?: number | string) {
  const normalizedId = String(id || '').trim()
  if (!normalizedId) return
  void router.push(`/gallery/detail/${encodeURIComponent(normalizedId)}`)
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
    const res = await adminFetchPictureBatch({
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
  void router.push('/gallery/manage')
}

onMounted(() => {
  void ensureAdmin()
})
</script>

<style scoped>
.import-auth-loading { display: block; min-height: 180px; padding-top: 70px; text-align: center; }
.import-alert { margin-bottom: 18px; }
.import-page-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; padding: 24px 0 16px; border-bottom: 1px solid var(--proto-line); }
.import-page-heading > div:first-child { display: flex; align-items: baseline; gap: 12px; min-width: 0; }
.import-page-heading h1 { margin: 0; font-size: 42px; line-height: 1; letter-spacing: -.06em; }
.import-role-tag.ant-tag { margin: 0; border: 0; border-radius: 4px; background: rgba(255,137,106,.18); color: var(--proto-ink); font-size: 11px; font-weight: 700; }
.import-limit { width: 150px; min-width: 150px; min-height: 68px; padding: 11px 13px; display: grid; grid-template-columns: 1fr auto; grid-template-rows: auto 1fr; column-gap: 10px; background: var(--proto-acid); }
.import-limit span, .import-limit strong, .import-limit small { display: block; }
.import-limit span { font-size: 10px; }
.import-limit strong { grid-column: 2; grid-row: 1 / span 2; align-self: center; font-size: 40px; line-height: .8; letter-spacing: -.08em; }
.import-limit small { grid-column: 1; align-self: end; font-size: 10px; opacity: .65; }
.import-layout { display: grid; grid-template-columns: minmax(320px, .9fr) minmax(0, 1.1fr); gap: var(--prototype-layout-gap); align-items: start; padding-top: 18px; }
.import-form-card { padding: 20px; background: var(--proto-ink); color: var(--proto-paper); box-shadow: var(--proto-shadow); }
.import-section-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 17px; }
.import-section-heading h2 { margin: 0; color: inherit; font-size: 23px; line-height: 1.1; letter-spacing: -.05em; }
.import-section-note { color: rgba(241,242,237,.55); font-size: 11px; }
.import-form-card :deep(.ant-form-item) { margin-bottom: 12px; }
.import-form-card :deep(.ant-form-item-label) { padding-bottom: 5px; }
.import-form-card :deep(.ant-form-item-label > label) { color: var(--proto-paper); font-size: 12px; }
.import-form-card :deep(.ant-form-item-required > label::before) { color: var(--proto-orange); }
.import-two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 13px; }
.import-form-card :deep(.ant-input), .import-form-card :deep(.ant-input-number), .import-form-card :deep(.ant-input-affix-wrapper), .import-form-card :deep(.ant-select-selector), .import-form-card :deep(textarea.ant-input) { border-color: rgba(241,242,237,.25) !important; background: rgba(241,242,237,.09) !important; color: var(--proto-paper) !important; }
.import-form-card :deep(.ant-input::placeholder), .import-form-card :deep(.ant-input-number-input::placeholder) { color: rgba(241,242,237,.48); }
.import-form-card :deep(.ant-input-number-input) { color: var(--proto-paper); }
.import-form-card :deep(.ant-select-selection-placeholder), .import-form-card :deep(.ant-select-arrow) { color: rgba(241,242,237,.5); }
.import-tags { display: flex; flex-wrap: wrap; gap: 7px; min-height: 26px; }
.import-tags :deep(.ant-tag-checkable) { padding: 4px 9px; border-radius: 3px; color: rgba(241,242,237,.72); font-size: 11px; }
.import-tags :deep(.ant-tag-checkable-checked) { background: var(--proto-acid); color: var(--proto-ink); }
.import-no-options { color: rgba(241,242,237,.55); font-size: 11px; }
.import-submit-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 17px; padding-top: 14px; border-top: 1px solid rgba(241,242,237,.18); }
.import-submit-row > span { color: rgba(241,242,237,.55); font-size: 10px; line-height: 1.4; }
.import-submit { min-width: 128px; }
.import-result-panel { min-width: 0; }
.import-result-panel > .import-section-heading { margin-bottom: 12px; }
.import-result-main { min-height: 204px; padding: 20px; display: flex; flex-direction: column; justify-content: center; }
.import-result-empty { min-height: 160px; display: grid; place-items: center; }
.import-result-empty :deep(.ant-empty) { margin: 0; }
.import-result-complete > strong { display: block; margin-top: 10px; font-size: 27px; line-height: 1.1; letter-spacing: -.05em; }
.import-result-complete > small { display: block; margin-top: 10px; color: var(--proto-muted); font-size: 11px; line-height: 1.6; }
.import-status-tag.ant-tag { margin: 0; border: 0; border-radius: 4px; background: rgba(186,255,61,.35); color: var(--proto-ink); font-size: 10px; font-weight: 700; }
.import-result-stats { margin-top: 22px; padding-top: 13px; display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; border-top: 1px solid var(--proto-line); }
.import-result-stat { min-width: 0; }
.import-result-stat span { display: block; color: var(--proto-muted); font-size: 10px; }
.import-result-stat strong { display: block; margin-top: 4px; color: var(--proto-ink); font-size: 22px; line-height: 1; }
.import-picture-results { margin-top: 12px; padding: 16px; }
.import-picture-results-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.import-picture-results-head h3 { margin: 0; font-size: 18px; letter-spacing: -.04em; }
.import-picture-results-head > strong { color: var(--proto-orange); font-family: 'DM Mono', monospace; font-size: 13px; font-weight: 500; white-space: nowrap; }
.import-picture-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 9px; max-height: 326px; overflow: auto; }
.import-picture-card { min-width: 0; border: 1px solid var(--proto-line); background: rgba(255,255,255,.55); cursor: pointer; transition: border-color 160ms ease, background-color 160ms ease; }
.import-picture-card:hover { border-color: var(--proto-orange); background: rgba(255,255,255,.85); }
.import-picture-card:focus-visible { outline: 2px solid var(--proto-orange); outline-offset: 3px; }
.import-picture-media { aspect-ratio: 4 / 3; display: grid; place-items: center; overflow: hidden; background: rgba(20,24,27,.08); color: var(--proto-muted); font-size: 10px; }
.import-picture-media img { width: 100%; height: 100%; display: block; object-fit: cover; }
.import-picture-meta { min-width: 0; padding: 9px 10px 10px; }
.import-picture-meta strong, .import-picture-meta small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.import-picture-meta strong { color: var(--proto-ink); font-size: 12px; font-weight: 600; }
.import-picture-meta small { margin-top: 5px; color: var(--proto-muted); font-family: 'DM Mono', monospace; font-size: 9px; }
.import-picture-empty { margin: 12px 0 0; padding: 22px 16px; }
@media (max-width: 980px) { .import-layout { grid-template-columns: 1fr; } .import-picture-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
@media (max-width: 620px) { .import-page-heading { align-items: flex-start; flex-direction: column; } .import-limit { width: 100%; } .import-two-col, .import-picture-grid, .import-result-stats { grid-template-columns: 1fr; gap: 0; } .import-picture-card + .import-picture-card, .import-result-stat + .import-result-stat { margin-top: 10px; } .import-submit-row { align-items: stretch; flex-direction: column; } .import-submit { width: 100%; } }
</style>
