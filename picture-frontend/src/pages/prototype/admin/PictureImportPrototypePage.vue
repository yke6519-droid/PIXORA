<template>
  <div class="import-prototype">
    <section class="import-page-heading">
      <div class="import-heading-copy">
        <div class="import-title-row">
          <h1>批量抓图</h1>
          <a-tag class="import-limit-tag">单次最多抓取 {{ MAX_IMPORT_COUNT }} 张</a-tag>
        </div>
        <p>根据关键词批量抓取图片并保存到公共图库</p>
      </div>
      <a-button class="proto-button ghost-button import-manage-button" @click="openPictureManage">
        图片管理
      </a-button>
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
        <section class="import-form-card proto-surface proto-rounded">
          <div class="import-section-heading">
            <h2>配置抓取任务</h2>
            <span class="import-section-note">填写后开始执行</span>
          </div>
          <!-- 绑定表单模型后，Ant Design Vue 才会在点击提交时触发 finish 回调。 -->
          <a-form :model="form" layout="vertical" class="proto-form" @finish="submitImport">
            <div class="import-form-group">
              <div class="import-form-group-heading">
                <strong>抓取条件</strong>
                <span>决定搜索范围和任务规模</span>
              </div>
              <a-form-item label="搜索关键词" required>
                <a-input v-model:value="form.searchText" placeholder="例如：minimal architecture" />
              </a-form-item>
              <a-form-item label="抓取数量" required>
                <a-input-number
                  v-model:value="form.count"
                  :min="1"
                  :max="MAX_IMPORT_COUNT"
                  style="width: 100%"
                />
              </a-form-item>
            </div>

            <div class="import-form-group import-save-group">
              <div class="import-form-group-heading">
                <strong>保存设置</strong>
                <span>设置图片入库后的基础信息</span>
              </div>
              <a-form-item label="图片名称（可选）">
                <a-input v-model:value="form.name" placeholder="未填写时使用搜索关键词" />
              </a-form-item>
              <a-form-item label="公共主题（可选）">
                <a-select
                  v-model:value="form.categoryId"
                  allow-clear
                  :loading="optionsLoading"
                  placeholder="选择公共主题（可不填）"
                  style="width: 100%"
                >
                  <a-select-option v-for="category in categories" :key="category.id" :value="category.id">
                    {{ category.categoryName }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </div>

            <p class="import-helper-note">
              图片会保存到公共图库；个人空间标签可在图片入库后单独管理。
            </p>
            <div class="import-submit-row">
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
            <div>
              <h2>抓取结果</h2>
              <p>成功入库的图片会显示在这里</p>
            </div>
            <span v-if="hasImportResult" class="import-result-count">
              {{ successCount }} / {{ targetCount }} 张
            </span>
          </div>

          <section class="import-result-workspace proto-surface proto-rounded">
            <div v-if="submitting" class="import-result-state import-loading-state">
              <a-spin size="large" tip="正在抓取并上传图片..." />
              <p>任务会依次搜索、下载并保存图片，请保持页面开启。</p>
            </div>
            <div v-else-if="!hasImportResult" class="import-result-state import-empty-state">
              <div class="import-empty-icon" aria-hidden="true">▧</div>
              <strong>暂无抓取结果</strong>
              <p>配置左侧任务并开始抓取，结果会显示在这里。</p>
            </div>

            <template v-else>
              <div class="import-result-summary">
                <div class="import-result-message">
                  <a-tag class="import-status-tag" :class="{ 'is-timeout': importTimedOut }">
                    {{ importTimedOut ? '异常停止' : '抓取完成' }}
                  </a-tag>
                  <div>
                    <strong>{{ importedMessage }}</strong>
                    <small>{{ lastExecutedAt }} · 关键词：{{ lastSearchText }}</small>
                  </div>
                </div>
                <div class="import-result-stats" aria-label="批量抓图结果统计">
                  <div class="import-result-stat">
                    <span>目标</span>
                    <strong>{{ targetCount }}</strong>
                  </div>
                  <div class="import-result-stat is-success">
                    <span>已入库</span>
                    <strong>{{ successCount }}</strong>
                  </div>
                  <div class="import-result-stat">
                    <span>未成功</span>
                    <strong>{{ pendingCount }}</strong>
                  </div>
                </div>
              </div>

              <div v-if="importedPictures.length" class="import-picture-grid">
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
                    <a-tag class="import-picture-status">已入库</a-tag>
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
              <a-empty v-else class="import-picture-empty" description="本次没有返回可展示的图片" />
            </template>
          </section>
        </section>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { adminFetchPictureBatch } from '../../../api/pictureController'
import { listCategory } from '../../../api/categoryController'
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
const categories = ref<API.Category[]>([])
const importedMessage = ref('等待执行')
const importedPictures = ref<API.PictureVO[]>([])
const targetCount = ref(0)
const successCount = ref(0)
const importTimedOut = ref(false)
const hasImportResult = ref(false)
const lastSearchText = ref('')
const lastExecutedAt = ref('')
// 前端展示和校验共用同一个上限；后端仍会再次校验，避免绕过页面直接提交非法数量。
const MAX_IMPORT_COUNT = 20
const form = reactive<API.PictureUploadByBatchRequest>({
  searchText: 'minimal architecture',
  count: 6,
  name: '',
  categoryId: undefined,
})
const pendingCount = computed(() => Math.max(targetCount.value - successCount.value, 0))
const BATCH_IMPORT_TIMEOUT_MS = 60_000
let importAbortController: AbortController | null = null
let importTimeoutId: number | null = null

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
    const res = await listCategory()
    if (res.data?.code === 200) {
      categories.value = res.data.data || []
    } else {
      loadError.value = res.data?.message || '公共主题加载失败，仍可提交无主题任务'
    }
  } catch (error: any) {
    loadError.value = error?.response?.data?.message || error?.message || '公共主题加载失败，仍可提交无主题任务'
  } finally {
    optionsLoading.value = false
  }
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
  if (!Number.isInteger(count) || count < 1 || count > MAX_IMPORT_COUNT) {
    message.warning(`抓取数量必须是 1 到 ${MAX_IMPORT_COUNT} 之间的整数`)
    return
  }

  submitting.value = true
  // 新任务开始时清除上一次结果，避免用户误把旧图片当成本次返回结果。
  importedPictures.value = []
  targetCount.value = 0
  successCount.value = 0
  importTimedOut.value = false
  hasImportResult.value = false
  importedMessage.value = '正在执行抓取任务'
  const abortController = new AbortController()
  importAbortController = abortController
  importTimeoutId = window.setTimeout(() => {
    abortController.abort()
  }, BATCH_IMPORT_TIMEOUT_MS)
  try {
    const res = await adminFetchPictureBatch({
      searchText,
      count,
      name: form.name?.trim() || undefined,
      categoryId: form.categoryId || undefined,
    }, {
      signal: abortController.signal,
      timeout: BATCH_IMPORT_TIMEOUT_MS,
    })
    if (res.data?.code !== 200) throw new Error(res.data?.message || '批量抓图失败')
    const result = res.data.data
    const pictures = (result?.pictureList || []).filter(
      (picture): picture is API.PictureVO => Boolean(picture),
    )
    targetCount.value = Number(result?.targetCount || 0)
    successCount.value = Number(result?.successCount || 0)
    importTimedOut.value = Boolean(result?.timedOut)
    importedPictures.value = pictures
    hasImportResult.value = true
    if (importTimedOut.value) {
      importedMessage.value = `任务超时，已保留 ${successCount.value} 张图片`
    } else {
      importedMessage.value = successCount.value
        ? `成功入库 ${successCount.value} / 处理 ${targetCount.value} 张`
        : '本次没有成功落库的图片'
    }
    lastSearchText.value = searchText
    lastExecutedAt.value = new Date().toLocaleString('zh-CN', { hour12: false })
    if (importTimedOut.value) {
      message.warning(`任务超过1分钟，已停止后续抓取，已保留 ${successCount.value} 张图片`)
    } else if (successCount.value > 0) {
      message.success(`批量抓图完成，成功入库 ${successCount.value} 张`)
    } else {
      message.warning('批量抓图完成，但没有图片成功落库')
    }
  } catch (error: any) {
    if (abortController.signal.aborted || error?.code === 'ERR_CANCELED') {
      importTimedOut.value = true
      loadError.value = '任务超过1分钟，已中断后续请求；已经入库的图片仍然保留'
      message.warning(loadError.value)
    } else {
      loadError.value = error?.response?.data?.message || error?.message || '批量抓图失败，请稍后重试'
      message.error(loadError.value)
    }
  } finally {
    if (importTimeoutId !== null) {
      window.clearTimeout(importTimeoutId)
      importTimeoutId = null
    }
    if (importAbortController === abortController) {
      importAbortController = null
    }
    submitting.value = false
  }
}

function openPictureManage() {
  void router.push('/gallery/manage')
}

onMounted(() => {
  void ensureAdmin()
})

onUnmounted(() => {
  if (importTimeoutId !== null) {
    window.clearTimeout(importTimeoutId)
  }
  importAbortController?.abort()
})
</script>

<style scoped>
.import-prototype {
  min-width: 0;
  color: var(--proto-ink);
  font-family: 'Geist', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.import-auth-loading { display: block; min-height: 180px; padding-top: 70px; text-align: center; }
.import-alert { margin: 18px 0 0; }

.import-page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding: 28px 0 18px;
  border-bottom: 1px solid var(--proto-line);
}

.import-heading-copy { min-width: 0; }
.import-title-row { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; }
.import-page-heading h1 { margin: 0; font-size: 40px; line-height: 1; letter-spacing: -.035em; font-weight: 800; }
.import-page-heading p { margin: 10px 0 0; color: var(--proto-muted); font-size: 13px; line-height: 1.55; }
.import-limit-tag.ant-tag {
  margin: 0;
  padding: 3px 10px;
  border: 1px solid rgba(91, 138, 0, .16);
  border-radius: 999px;
  background: rgba(186, 255, 61, .16);
  color: #527b09;
  font-size: 11px;
  font-weight: 700;
  line-height: 22px;
}
.import-manage-button { flex: 0 0 auto; }

.import-layout {
  display: grid;
  grid-template-columns: minmax(380px, 410px) minmax(0, 1fr);
  gap: 24px;
  align-items: start;
  padding-top: 20px;
}

.import-form-card {
  padding: 22px;
  background: rgba(255, 255, 255, .78);
  box-shadow: 0 8px 28px rgba(18, 23, 23, .07);
}

.import-section-heading { display: flex; align-items: center; justify-content: space-between; gap: 14px; margin-bottom: 18px; }
.import-section-heading h2 { margin: 0; color: var(--proto-ink); font-size: 20px; line-height: 1.25; letter-spacing: -.025em; font-weight: 800; }
.import-section-heading p { margin: 5px 0 0; color: var(--proto-muted); font-size: 12px; line-height: 1.5; }
.import-section-note { color: var(--proto-muted); font-size: 11px; white-space: nowrap; }

.import-form-group + .import-form-group { margin-top: 20px; padding-top: 20px; border-top: 1px solid var(--proto-line); }
.import-form-group-heading { margin-bottom: 13px; }
.import-form-group-heading strong,
.import-form-group-heading span { display: block; }
.import-form-group-heading strong { color: var(--proto-ink); font-size: 14px; font-weight: 800; }
.import-form-group-heading span { margin-top: 3px; color: var(--proto-muted); font-size: 11px; line-height: 1.5; }
.import-form-card :deep(.ant-form-item) { margin-bottom: 13px; }
.import-form-card :deep(.ant-form-item:last-child) { margin-bottom: 0; }
.import-form-card :deep(.ant-form-item-label) { padding-bottom: 5px; }
.import-form-card :deep(.ant-form-item-label > label) { color: var(--proto-ink); font-size: 12px; font-weight: 700; }
.import-form-card :deep(.ant-form-item-required > label::before) { color: #b35c32; }
.import-form-card :deep(.ant-input),
.import-form-card :deep(.ant-input-number),
.import-form-card :deep(.ant-select-selector) {
  min-height: 40px;
  border-color: rgba(17, 20, 22, .14) !important;
  border-radius: 8px !important;
  background: #fff !important;
  color: var(--proto-ink) !important;
  box-shadow: none !important;
  font-family: inherit;
  font-size: 13px;
}
.import-form-card :deep(.ant-input-number-input) { height: 38px; color: var(--proto-ink); }
.import-form-card :deep(.ant-select-selector) { display: flex; align-items: center; }
.import-form-card :deep(.ant-input:hover),
.import-form-card :deep(.ant-input-number:hover),
.import-form-card :deep(.ant-select-selector:hover) { border-color: rgba(79, 121, 0, .55) !important; }

.import-helper-note {
  margin: 18px 0 0;
  padding: 11px 12px;
  border: 1px solid rgba(91, 138, 0, .14);
  border-radius: 8px;
  background: rgba(186, 255, 61, .08);
  color: #596056;
  font-size: 11px;
  line-height: 1.6;
}
.import-submit-row { margin-top: 18px; padding-top: 18px; border-top: 1px solid var(--proto-line); }
.import-submit { width: 100%; }

.import-result-panel { min-width: 0; }
.import-result-panel > .import-section-heading { min-height: 40px; margin-bottom: 10px; }
.import-result-count { color: var(--proto-muted); font-size: 12px; font-weight: 700; white-space: nowrap; }
.import-result-workspace {
  min-height: 560px;
  padding: 20px;
  background: rgba(255, 255, 255, .78);
  box-shadow: 0 8px 28px rgba(18, 23, 23, .07);
}

.import-result-state { min-height: 516px; display: flex; align-items: center; justify-content: center; flex-direction: column; text-align: center; }
.import-result-state p { max-width: 42ch; margin: 9px 0 0; color: var(--proto-muted); font-size: 12px; line-height: 1.6; }
.import-loading-state :deep(.ant-spin-text) { margin-top: 9px; color: var(--proto-ink); font-size: 13px; font-weight: 700; }
.import-empty-icon { width: 48px; height: 48px; display: grid; place-items: center; margin-bottom: 15px; border: 1px solid var(--proto-line); border-radius: 12px; background: rgba(17, 20, 22, .035); color: #697171; font-size: 25px; }
.import-empty-state > strong { font-size: 16px; font-weight: 800; }

.import-result-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
  padding-bottom: 17px;
  border-bottom: 1px solid var(--proto-line);
}
.import-result-message { min-width: 0; display: flex; align-items: center; gap: 12px; }
.import-result-message > div { min-width: 0; }
.import-result-message strong,
.import-result-message small { display: block; }
.import-result-message strong { color: var(--proto-ink); font-size: 15px; font-weight: 800; line-height: 1.45; }
.import-result-message small { margin-top: 3px; overflow: hidden; color: var(--proto-muted); font-size: 11px; line-height: 1.5; text-overflow: ellipsis; white-space: nowrap; }
.import-status-tag.ant-tag { margin: 0; padding: 2px 9px; border: 0; border-radius: 999px; background: rgba(186,255,61,.24); color: #527b09; font-size: 10px; font-weight: 800; white-space: nowrap; }
.import-status-tag.is-timeout.ant-tag { background: rgba(255, 137, 106, .16); color: #9a4b31; }

.import-result-stats { flex: 0 0 auto; display: flex; align-items: center; gap: 0; }
.import-result-stat { min-width: 68px; padding: 0 14px; border-left: 1px solid var(--proto-line); }
.import-result-stat span,
.import-result-stat strong { display: block; }
.import-result-stat span { color: var(--proto-muted); font-size: 10px; }
.import-result-stat strong { margin-top: 3px; color: var(--proto-ink); font-size: 18px; line-height: 1; }
.import-result-stat.is-success strong { color: #649b08; }

.import-picture-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(170px, 1fr)); gap: 12px; }
.import-picture-card {
  min-width: 0;
  overflow: hidden;
  border: 1px solid rgba(17, 20, 22, .11);
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  transition: border-color 160ms ease, box-shadow 160ms ease;
}
.import-picture-card:hover { border-color: rgba(79, 121, 0, .46); box-shadow: 0 7px 18px rgba(18, 23, 23, .09); }
.import-picture-card:focus-visible { outline: 2px solid #4f7900; outline-offset: 3px; }
.import-picture-media { aspect-ratio: 4 / 3; display: grid; place-items: center; overflow: hidden; position: relative; background: #e9ebe6; color: var(--proto-muted); font-size: 10px; }
.import-picture-media img { width: 100%; height: 100%; display: block; object-fit: cover; }
.import-picture-status.ant-tag { position: absolute; top: 8px; right: 8px; margin: 0; padding: 1px 7px; border: 0; border-radius: 999px; background: rgba(255, 255, 255, .9); color: #527b09; font-size: 9px; font-weight: 800; box-shadow: 0 2px 8px rgba(18, 23, 23, .1); }
.import-picture-meta { min-width: 0; padding: 10px 11px 11px; }
.import-picture-meta strong,
.import-picture-meta small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.import-picture-meta strong { color: var(--proto-ink); font-size: 12px; font-weight: 700; }
.import-picture-meta small { margin-top: 5px; color: var(--proto-muted); font-family: 'DM Mono', monospace; font-size: 9px; }
.import-picture-empty { min-height: 390px; display: grid; place-items: center; }

@media (max-width: 1100px) {
  .import-layout { grid-template-columns: minmax(340px, 370px) minmax(0, 1fr); }
  .import-picture-grid { grid-template-columns: repeat(auto-fill, minmax(155px, 1fr)); }
  .import-result-summary { align-items: flex-start; flex-direction: column; }
  .import-result-stat:first-child { padding-left: 0; border-left: 0; }
}

@media (max-width: 880px) {
  .import-layout { grid-template-columns: 1fr; }
  .import-result-workspace { min-height: 460px; }
  .import-result-state { min-height: 416px; }
}

@media (max-width: 620px) {
  .import-page-heading { align-items: stretch; flex-direction: column; gap: 15px; padding-top: 20px; }
  .import-page-heading h1 { font-size: 34px; }
  .import-manage-button { width: 100%; }
  .import-form-card,
  .import-result-workspace { padding: 16px; }
  .import-section-heading { align-items: flex-start; }
  .import-result-summary { gap: 15px; }
  .import-result-message { align-items: flex-start; flex-direction: column; gap: 8px; }
  .import-result-message small { white-space: normal; }
  .import-result-stats { width: 100%; justify-content: space-between; }
  .import-result-stat { flex: 1; min-width: 0; padding-inline: 11px; }
  .import-picture-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 9px; }
}

@media (max-width: 420px) {
  .import-picture-grid { grid-template-columns: 1fr; }
}
</style>
