<template>
  <div class="gallery-prototype">
    <div class="gallery-layout">
      <aside class="gallery-sidebar proto-surface" aria-label="图片分类">
        <div class="gallery-sidebar-head">
          <h2>全部分类</h2>
          <UpOutlined aria-hidden="true" />
        </div>

        <nav class="gallery-category-list">
          <button
            type="button"
            class="gallery-category-item"
            :class="{ 'is-active': !category }"
            @click="selectCategory('')"
          >
            <AppstoreOutlined aria-hidden="true" />
            <span>全部图片</span>
            <small>{{ categoryCountLoading ? '…' : formatCount(allPictureTotal) }}</small>
          </button>

          <!-- 分类接口只返回名称，数量由公共图库分页接口按分类统计后展示。 -->
          <button
            v-for="item in categories"
            :key="item"
            type="button"
            class="gallery-category-item"
            :class="{ 'is-active': category === item }"
            @click="selectCategory(item)"
          >
            <component :is="categoryIcon(item)" aria-hidden="true" />
            <span>{{ item }}</span>
            <small>{{ categoryCountLoading ? '…' : formatCount(categoryCounts[item] ?? null) }}</small>
          </button>

          <span v-if="!categories.length" class="gallery-muted gallery-category-empty">暂无可用分类</span>
        </nav>
      </aside>

      <main class="gallery-main-column">
        <section class="gallery-search-area" aria-label="图库搜索">
          <div class="gallery-search-box">
            <a-input
              v-model:value="searchText"
              class="gallery-search-input"
              placeholder="搜索图片名称、关键词或描述..."
              @pressEnter="runSearch"
            >
              <template #prefix>
                <SearchOutlined aria-hidden="true" />
              </template>
            </a-input>
            <span class="gallery-search-settings" aria-hidden="true">
              <SettingOutlined />
            </span>
            <a-button class="gallery-search-button" type="primary" @click="runSearch">搜索</a-button>
          </div>
        </section>

        <section class="gallery-toolbar" aria-label="图库筛选和视图设置">
          <div class="gallery-tag-list" aria-label="按标签筛选">
            <a-checkable-tag
              v-for="tag in visibleTags"
              :key="tag"
              :checked="selectedTags.includes(tag)"
              @change="(checked: boolean) => toggleTag(tag, checked)"
            >
              {{ tag }}
            </a-checkable-tag>
            <button
              v-if="tags.length > 8"
              type="button"
              class="gallery-more-tags"
              @click="showAllTags = !showAllTags"
            >
              {{ showAllTags ? '收起' : '更多' }}
              <DownOutlined :class="{ 'is-open': showAllTags }" aria-hidden="true" />
            </button>
            <span v-if="!tags.length" class="gallery-muted">暂无可用标签</span>
          </div>

          <div class="gallery-toolbar-actions">
            <a-button
              class="gallery-tool-button"
              :class="{ 'is-active': filterOpen || hasActiveFilters }"
              @click="filterOpen = !filterOpen"
            >
              <FilterOutlined aria-hidden="true" />
              <span>筛选</span>
            </a-button>
            <a-select v-model:value="sortOrder" class="gallery-sort-select" @change="runSearch">
              <a-select-option value="latest">综合排序</a-select-option>
              <a-select-option value="size">尺寸优先</a-select-option>
            </a-select>
            <div class="gallery-view-toggle" aria-label="切换视图">
              <button
                type="button"
                class="gallery-view-button"
                :class="{ 'is-active': viewMode === 'grid' }"
                aria-label="网格视图"
                @click="viewMode = 'grid'"
              >
                <AppstoreOutlined aria-hidden="true" />
              </button>
              <button
                type="button"
                class="gallery-view-button"
                :class="{ 'is-active': viewMode === 'list' }"
                aria-label="列表视图"
                @click="viewMode = 'list'"
              >
                <MenuOutlined aria-hidden="true" />
              </button>
            </div>
          </div>
        </section>

        <section v-if="filterOpen" class="gallery-filter-panel proto-surface" aria-label="当前筛选条件">
          <div>
            <strong>当前筛选</strong>
            <span>{{ filterSummary }}</span>
          </div>
          <a-button class="proto-button ghost-button" @click="resetFilters">重置筛选</a-button>
        </section>

        <section class="gallery-results-bar">
          <div class="gallery-result-summary" aria-live="polite">
            <span class="result-count">{{ total.toLocaleString() }}</span>
            <span class="gallery-result-label">张图片</span>
          </div>
        </section>

        <a-alert v-if="loadError" class="gallery-alert" type="error" show-icon :message="loadError" />

        <a-spin :spinning="loading" tip="加载图片中...">
          <section
            v-if="pictureList.length"
            class="gallery-grid"
            :class="{ 'is-list': viewMode === 'list' }"
            aria-label="公共图片列表"
          >
            <PictureGalleryCard
              v-for="picture in pictureList"
              :key="picture.id"
              :picture="picture"
              :view-mode="viewMode"
              @open="openDetail(picture.id)"
            />
          </section>
          <div v-else-if="!loading" class="gallery-empty-state">
            <a-empty description="没有找到符合条件的图片">
              <template #extra>
                <a-button class="proto-button acid-button" type="primary" @click="router.push('/gallery/upload')">
                  上传第一张图片
                </a-button>
              </template>
            </a-empty>
          </div>
        </a-spin>

        <div v-if="total > 0" class="gallery-pagination">
          <span class="gallery-pagination-caption">第 {{ current }} 页 · 共 {{ total.toLocaleString() }} 张</span>
          <a-pagination
            v-model:current="current"
            v-model:pageSize="pageSize"
            :total="total"
            :page-size-options="pageSizeOptions"
            show-size-changer
            show-less-items
            :show-total="(value: number) => `共 ${value.toLocaleString()} 张`"
            @change="handlePageChange"
            @show-size-change="handlePageSizeChange"
          />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  AppstoreOutlined,
  BoxPlotOutlined,
  CameraOutlined,
  DownOutlined,
  EditOutlined,
  FilterOutlined,
  MenuOutlined,
  PictureOutlined,
  SearchOutlined,
  SettingOutlined,
  SmileOutlined,
  UpOutlined,
} from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import { queryPicturePageCache, listPictureCategory } from '../../../api/pictureController'
import { useLoginUserStore } from '../../../stores/useLoginUserStore'
import PictureGalleryCard from './components/PictureGalleryCard.vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const loading = ref(false)
const loadError = ref('')
const pictureList = ref<API.PictureVO[]>([])
const total = ref(0)
const allPictureTotal = ref<number | null>(null)
const categoryCounts = ref<Record<string, number>>({})
const categoryCountLoading = ref(false)
const current = ref(1)
const pageSize = ref(10)
const searchText = ref('')
const category = ref('')
const selectedTags = ref<string[]>([])
const categories = ref<string[]>([])
const tags = ref<string[]>([])
const sortOrder = ref<'latest' | 'size'>('latest')
const viewMode = ref<'grid' | 'list'>('grid')
const showAllTags = ref(false)
const filterOpen = ref(false)

const isLoggedIn = computed(() => Boolean(loginUserStore.loginUser))
const maxPageSize = computed(() => isLoggedIn.value ? 20 : 10)
const pageSizeOptions = computed(() => isLoggedIn.value ? ['10', '20'] : ['10'])
const visibleTags = computed(() => showAllTags.value ? tags.value : tags.value.slice(0, 8))
const hasActiveFilters = computed(() => Boolean(searchText.value.trim() || category.value || selectedTags.value.length))
const filterSummary = computed(() => {
  const values: string[] = []
  if (searchText.value.trim()) values.push(`关键词：${searchText.value.trim()}`)
  if (category.value) values.push(`分类：${category.value}`)
  if (selectedTags.value.length) values.push(`标签：${selectedTags.value.join('、')}`)
  return values.length ? values.join(' · ') : '未设置额外筛选'
})

async function loadGallery() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await queryPicturePageCache({
      current: current.value,
      pageSize: pageSize.value,
      searchText: searchText.value.trim() || undefined,
      category: category.value || undefined,
      tags: selectedTags.value.length ? [...selectedTags.value] : undefined,
      pictureCheck: 1,
      spaceId: 0,
      sortFiled: sortOrder.value === 'size' ? 'picsize' : 'createtime',
      sortOrder: 'descend',
    })
    if (res.data?.code !== 200) {
      throw new Error(res.data?.message || '公共图库加载失败')
    }
    pictureList.value = res.data.data?.pictureList || []
    // 后端 Long 可能序列化为字符串，分页组件的 total 必须使用数字。
    total.value = Number(res.data.data?.total || 0)
  } catch (error: any) {
    pictureList.value = []
    total.value = 0
    loadError.value = error?.response?.data?.message || error?.message || '公共图库加载失败，请确认后端服务和 Redis 已启动'
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    const res = await listPictureCategory()
    if (res.data?.code === 200) {
      categories.value = res.data.data?.categorys || []
      tags.value = res.data.data?.tags || []
      await loadCategoryCounts()
    }
  } catch {
    message.warning('分类和标签暂时加载失败，仍可使用关键词查询')
  }
}

/**
 * 查询公共图库的总数。
 * 统计请求只取1条记录，total仍由后端分页结果提供，不把当前页面图片数量当成分类总数。
 */
async function queryPublicPictureCount(categoryName?: string) {
  const res = await queryPicturePageCache({
    current: 1,
    pageSize: 1,
    category: categoryName || undefined,
    pictureCheck: 1,
    spaceId: 0,
    sortFiled: 'createtime',
    sortOrder: 'descend',
  })
  if (res.data?.code !== 200) {
    throw new Error(res.data?.message || '公共图库分类数量加载失败')
  }
  return Number(res.data.data?.total || 0)
}

/**
 * 单独维护“全部图片”和各分类的数量，避免它们被当前搜索条件或选中分类覆盖。
 */
async function loadCategoryCounts() {
  categoryCountLoading.value = true
  try {
    const categoryNames = categories.value.filter(Boolean)
    const [allCount, ...categoryEntries] = await Promise.all([
      queryPublicPictureCount(),
      ...categoryNames.map(async (categoryName) => [
        categoryName,
        await queryPublicPictureCount(categoryName),
      ] as const),
    ])
    allPictureTotal.value = allCount
    categoryCounts.value = Object.fromEntries(categoryEntries)
  } catch {
    // 数量加载失败不影响图片浏览，左侧仅显示占位符，避免展示错误统计。
    allPictureTotal.value = null
    categoryCounts.value = {}
  } finally {
    categoryCountLoading.value = false
  }
}

function formatCount(value: number | null) {
  return value === null ? '—' : value.toLocaleString()
}

function categoryIcon(name: string) {
  if (name.includes('摄影')) return CameraOutlined
  if (name.includes('插画') || name.includes('向量')) return EditOutlined
  if (name.includes('3D')) return BoxPlotOutlined
  if (name.includes('表情')) return SmileOutlined
  if (name.includes('壁纸')) return PictureOutlined
  return AppstoreOutlined
}

function runSearch() {
  current.value = 1
  void loadGallery()
}

function selectCategory(value: string) {
  category.value = value
  runSearch()
}

function toggleTag(tag: string, checked: boolean) {
  selectedTags.value = checked
    ? [...new Set([...selectedTags.value, tag])]
    : selectedTags.value.filter((item) => item !== tag)
  runSearch()
}

function resetFilters() {
  searchText.value = ''
  category.value = ''
  selectedTags.value = []
  sortOrder.value = 'latest'
  showAllTags.value = false
  filterOpen.value = false
  current.value = 1
  void loadGallery()
}

function handlePageChange(page: number, size: number) {
  current.value = page
  pageSize.value = Math.min(size || pageSize.value, maxPageSize.value)
  void loadGallery()
}

function handlePageSizeChange(page: number, size: number) {
  current.value = page
  pageSize.value = Math.min(size || pageSize.value, maxPageSize.value)
  void loadGallery()
}

function openDetail(id?: number | string) {
  const normalizedId = String(id || '').trim()
  if (normalizedId) router.push(`/gallery/detail/${encodeURIComponent(normalizedId)}`)
}

watch(
  () => loginUserStore.loginUser?.id,
  (userId, previousUserId) => {
    if (userId === previousUserId) return
    pageSize.value = userId ? 20 : 10
    current.value = 1
    void loadGallery()
  },
)

onMounted(() => {
  // 登录用户首屏加载20张，未登录用户首屏加载10张，与后端公共图库上限保持一致。
  pageSize.value = isLoggedIn.value ? 20 : 10
  void Promise.all([loadCategories(), loadGallery()])
})
</script>

<style scoped>
.gallery-prototype { min-width: 0; padding-bottom: 24px; }
.gallery-layout { display: grid; grid-template-columns: minmax(190px, .18fr) minmax(0, .82fr); gap: 24px; padding-top: 18px; align-items: start; }
.gallery-sidebar { position: sticky; top: calc(var(--prototype-topbar-height, 52px) + 18px); min-width: 0; padding: 16px 12px; border: 1px solid rgba(17,20,22,.05); border-radius: 14px; background: rgba(255,255,255,.66); box-shadow: 0 12px 30px rgba(18,23,23,.035); }
.gallery-sidebar-head { min-height: 38px; padding: 0 10px 10px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid rgba(17,20,22,.08); }
.gallery-sidebar-head h2 { margin: 0; font-size: 14px; font-weight: 800; letter-spacing: -.03em; }
.gallery-sidebar-head :deep(svg) { color: var(--proto-muted); font-size: 13px; }
.gallery-category-list { padding-top: 10px; display: flex; flex-direction: column; gap: 3px; }
.gallery-category-item { min-height: 42px; padding: 0 10px; display: grid; grid-template-columns: 20px minmax(0, 1fr) auto; align-items: center; gap: 9px; border: 0; border-radius: 9px; background: transparent; color: var(--proto-muted); cursor: pointer; font-family: inherit; font-size: 12px; text-align: left; transition: background .2s ease, color .2s ease; }
.gallery-category-item :deep(svg) { color: var(--proto-muted); font-size: 16px; }
.gallery-category-item span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.gallery-category-item small { color: var(--proto-muted); font-family: 'DM Mono', monospace; font-size: 10px; }
.gallery-category-item:hover { background: rgba(241,242,237,.8); color: var(--proto-ink); }
.gallery-category-item.is-active { background: rgba(186,255,61,.12); color: #67951f; font-weight: 700; }
.gallery-category-item.is-active :deep(svg), .gallery-category-item.is-active small { color: #67951f; }
.gallery-category-empty { display: block; padding: 12px 10px; }

.gallery-main-column { min-width: 0; }
.gallery-search-area { padding: 0 0 12px; }
.gallery-search-box { min-height: 50px; display: grid; grid-template-columns: minmax(0, 1fr) auto auto; align-items: center; overflow: hidden; border: 1px solid rgba(17,20,22,.12); border-radius: 11px; background: rgba(255,255,255,.78); box-shadow: 0 10px 24px rgba(18,23,23,.05); }
.gallery-search-input { min-width: 0; }
.gallery-search-input :deep(.ant-input), .gallery-search-input :deep(.ant-input-affix-wrapper) { height: 50px !important; border: 0 !important; background: transparent !important; box-shadow: none !important; font-family: 'Manrope', Arial, sans-serif; font-size: 13px; }
.gallery-search-input :deep(.ant-input-prefix) { margin-inline-end: 10px; color: var(--proto-muted); font-size: 17px; }
.gallery-search-settings { padding: 0 13px; color: var(--proto-muted); font-size: 15px; }
.gallery-search-button.ant-btn { height: 50px; padding-inline: 23px; border: 0; border-radius: 0 10px 10px 0; background: var(--proto-ink); color: var(--proto-paper); font-size: 13px; font-weight: 700; }
.gallery-search-button.ant-btn:hover { background: var(--proto-orange); color: var(--proto-ink); }

.gallery-toolbar { min-height: 48px; padding: 0 0 12px; display: flex; align-items: center; justify-content: space-between; gap: 18px; }
.gallery-tag-list { min-width: 0; display: flex; flex: 1 1 auto; align-items: center; flex-wrap: nowrap; gap: 7px; overflow: hidden; }
.gallery-tag-list :deep(.ant-tag-checkable) { display: inline-flex; height: 29px; align-items: center; margin: 0; padding: 0 11px; border: 1px solid rgba(17,20,22,.13); border-radius: 7px; background: rgba(255,255,255,.46); color: var(--proto-muted); font-family: 'Manrope', Arial, sans-serif; font-size: 12px; line-height: 29px; transition: background .2s ease, border-color .2s ease, color .2s ease; }
.gallery-tag-list :deep(.ant-tag-checkable:hover) { border-color: var(--proto-ink); color: var(--proto-ink); }
.gallery-tag-list :deep(.ant-tag-checkable-checked) { border-color: var(--proto-acid); background: var(--proto-acid); color: var(--proto-ink); }
.gallery-more-tags { flex: 0 0 auto; padding: 0 4px; border: 0; background: transparent; color: var(--proto-muted); cursor: pointer; font-family: inherit; font-size: 12px; }
.gallery-more-tags:hover { color: var(--proto-ink); }
.gallery-more-tags :deep(svg) { margin-left: 3px; font-size: 10px; transition: transform .2s ease; }
.gallery-more-tags :deep(svg.is-open) { transform: rotate(180deg); }
.gallery-toolbar-actions { display: flex; flex: 0 0 auto; align-items: center; gap: 9px; }
.gallery-tool-button.ant-btn { height: 32px; padding-inline: 12px; border-color: rgba(17,20,22,.11); border-radius: 8px; background: rgba(255,255,255,.66); color: var(--proto-muted); font-size: 12px; }
.gallery-tool-button.ant-btn :deep(svg) { margin-right: 5px; }
.gallery-tool-button.ant-btn:hover, .gallery-tool-button.is-active { border-color: var(--proto-ink); color: var(--proto-ink); }
.gallery-sort-select { width: 124px; }
.gallery-sort-select :deep(.ant-select-selector) { height: 32px !important; align-items: center; border-color: rgba(17,20,22,.11) !important; border-radius: 8px !important; background: rgba(255,255,255,.66) !important; box-shadow: none !important; font-size: 12px; }
.gallery-view-toggle { height: 32px; padding: 3px; display: inline-flex; align-items: center; gap: 2px; border: 1px solid rgba(17,20,22,.11); border-radius: 8px; background: rgba(255,255,255,.66); }
.gallery-view-button { width: 29px; height: 24px; display: grid; place-items: center; border: 0; border-radius: 5px; background: transparent; color: var(--proto-muted); cursor: pointer; }
.gallery-view-button:hover, .gallery-view-button.is-active { background: var(--proto-acid); color: var(--proto-ink); }
.gallery-view-button :deep(svg) { font-size: 15px; }
.gallery-filter-panel { min-height: 48px; margin-bottom: 12px; padding: 10px 13px; display: flex; align-items: center; justify-content: space-between; gap: 12px; border: 1px solid rgba(17,20,22,.08); border-radius: 10px; background: rgba(255,255,255,.62); }
.gallery-filter-panel > div { min-width: 0; display: flex; align-items: baseline; gap: 10px; }
.gallery-filter-panel strong { flex: 0 0 auto; font-size: 12px; }
.gallery-filter-panel span { overflow: hidden; color: var(--proto-muted); font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.gallery-filter-panel .ant-btn { flex: 0 0 auto; height: 28px; padding-inline: 10px; border-radius: 7px; font-size: 11px; }

.gallery-results-bar { min-height: 45px; display: flex; align-items: center; }
.gallery-result-summary { display: flex; align-items: baseline; gap: 8px; }
.result-count { color: var(--proto-ink); font-family: 'Abril Fatface', Georgia, serif; font-size: 28px; font-weight: 400; letter-spacing: -.07em; line-height: 1; }
.gallery-result-label { color: var(--proto-muted); font-family: 'Manrope', Arial, sans-serif; font-size: 13px; font-weight: 600; }
.gallery-alert { margin-bottom: 14px; }

.gallery-grid { display: grid; grid-auto-flow: dense; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; }
.gallery-grid.is-list { grid-template-columns: 1fr; gap: 12px; }

.gallery-empty-state { padding: 46px 24px; border: 1px solid var(--proto-line); border-radius: 12px; background: rgba(255,255,255,.45); }
.gallery-empty-state :deep(.ant-empty-description) { color: var(--proto-muted); }
.gallery-pagination { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 18px 0 4px; border-top: 1px solid var(--proto-line); }
.gallery-pagination-caption { color: var(--proto-muted); font-family: 'Abril Fatface', Georgia, serif; font-size: 15px; font-weight: 400; line-height: 1; white-space: nowrap; }
.gallery-pagination :deep(.ant-pagination) { margin: 0; }
.gallery-pagination :deep(.ant-pagination-item), .gallery-pagination :deep(.ant-pagination-prev), .gallery-pagination :deep(.ant-pagination-next), .gallery-pagination :deep(.ant-pagination-jump-prev), .gallery-pagination :deep(.ant-pagination-jump-next) { width: 32px; height: 32px; margin-inline-end: 5px; line-height: 30px; }
.gallery-pagination :deep(.ant-pagination-item), .gallery-pagination :deep(.ant-pagination-prev .ant-pagination-item-link), .gallery-pagination :deep(.ant-pagination-next .ant-pagination-item-link) { border-radius: 7px; }
.gallery-pagination :deep(.ant-pagination-item a) { color: var(--proto-muted); font-family: 'Abril Fatface', Georgia, serif; font-size: 12px; }
.gallery-pagination :deep(.ant-pagination-item-active) { border-color: var(--proto-acid); background: var(--proto-acid); }
.gallery-pagination :deep(.ant-pagination-item-active a) { color: var(--proto-ink); }

@media (prefers-reduced-motion: reduce) {
  .gallery-more-tags :deep(svg) { transition: none; }
}
@media (max-width: 1120px) {
  .gallery-layout { grid-template-columns: minmax(170px, .22fr) minmax(0, .78fr); gap: 18px; }
  .gallery-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .gallery-tag-list { flex-wrap: wrap; overflow: visible; }
}
@media (max-width: 820px) {
  .gallery-layout { grid-template-columns: 1fr; }
  .gallery-sidebar { position: static; }
  .gallery-category-list { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .gallery-toolbar { align-items: flex-start; flex-direction: column; }
  .gallery-toolbar-actions { width: 100%; justify-content: flex-end; }
  .gallery-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 720px) {
  .gallery-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 600px) {
  .gallery-category-list { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .gallery-search-box { grid-template-columns: minmax(0, 1fr) auto; }
  .gallery-search-settings { display: none; }
  .gallery-search-button.ant-btn { padding-inline: 16px; }
  .gallery-toolbar-actions { justify-content: stretch; }
  .gallery-tool-button, .gallery-sort-select { flex: 1 1 0; }
  .gallery-view-toggle { flex: 0 0 auto; }
  .gallery-pagination { align-items: flex-start; flex-direction: column; }
}
@media (max-width: 520px) {
  .gallery-grid { grid-template-columns: 1fr; }
}
</style>
