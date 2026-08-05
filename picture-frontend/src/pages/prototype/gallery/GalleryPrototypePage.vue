<template>
  <div class="gallery-prototype">
    <section class="gallery-hero">
      <div class="gallery-hero-copy">
        <div class="gallery-brand">PIXORA</div>
        <h1 class="gallery-title">找到一张值得留下的图片。</h1>
      </div>
      <div class="gallery-hero-actions">
        <a-button class="proto-button ghost-button" @click="router.push('/prototype/gallery/manage')">管理我的图片</a-button>
        <a-button class="proto-button acid-button" type="primary" @click="router.push('/prototype/gallery/upload')">上传图片</a-button>
      </div>
    </section>

    <section class="gallery-search">
      <a-card class="gallery-filter-card proto-surface" :bordered="false">
        <div class="gallery-filter-row">
          <a-input-search
            v-model:value="searchText"
            class="gallery-search-input"
            placeholder="搜索图片名称或简介"
            enter-button="搜索"
            @search="runSearch"
          />
          <a-select v-model:value="category" class="gallery-category-select" placeholder="图片分类" @change="runSearch">
            <a-select-option value="">全部分类</a-select-option>
            <a-select-option v-for="item in categories" :key="item" :value="item">{{ item }}</a-select-option>
          </a-select>
          <a-button class="proto-button ghost-button gallery-reset-button" @click="resetFilters">重置</a-button>
        </div>
        <div class="gallery-tag-row" aria-label="按标签筛选">
          <a-checkable-tag
            v-for="tag in tags"
            :key="tag"
            :checked="selectedTags.includes(tag)"
            @change="(checked: boolean) => toggleTag(tag, checked)"
          >
            {{ tag }}
          </a-checkable-tag>
          <span v-if="!tags.length" class="gallery-muted">暂无可用标签</span>
        </div>
      </a-card>
    </section>

    <section class="gallery-results-bar">
      <div class="gallery-result-summary" aria-live="polite">
        <span class="result-count">{{ total }}</span>
        <span class="gallery-result-label">张公开图片</span>
      </div>
      <a-radio-group v-model:value="sortOrder" class="gallery-sort-group" button-style="solid" size="small" @change="runSearch">
        <a-radio-button value="latest">最近上传</a-radio-button>
        <a-radio-button value="size">尺寸优先</a-radio-button>
      </a-radio-group>
    </section>

    <a-alert v-if="loadError" class="gallery-alert" type="error" show-icon :message="loadError" />

    <a-spin :spinning="loading" tip="加载图片中...">
      <section v-if="pictureList.length" class="gallery-grid" aria-label="公共图片列表">
        <a-card
          v-for="picture in pictureList"
          :key="picture.id"
          class="gallery-card proto-surface"
          :bordered="false"
          role="button"
          tabindex="0"
          @click="openDetail(picture.id)"
          @keydown.enter="openDetail(picture.id)"
          @keydown.space.prevent="openDetail(picture.id)"
        >
          <div class="gallery-card-image">
            <img v-if="picture.thumbnailUrl || picture.url" :src="picture.thumbnailUrl || picture.url" :alt="picture.name || '公共图片'" loading="lazy" />
            <div v-else class="gallery-image-empty">暂无图片地址</div>
            <span class="gallery-card-overlay">查看详情</span>
          </div>
          <div class="gallery-card-body">
            <div class="gallery-card-title">
              <strong>{{ picture.name || '未命名图片' }}</strong>
              <span class="gallery-card-arrow" aria-hidden="true">↗</span>
            </div>
            <p>{{ picture.introduction || '暂无图片简介' }}</p>
            <div class="gallery-card-foot">
              <a-tag v-if="picture.category" class="gallery-category-tag">{{ picture.category }}</a-tag>
              <span v-else class="gallery-muted">未分类</span>
              <span class="gallery-author">{{ picture.createdUser?.username || '未知用户' }}</span>
            </div>
          </div>
        </a-card>
      </section>
      <div v-else-if="!loading" class="gallery-empty-state">
        <a-empty description="没有找到符合条件的图片">
          <template #extra>
            <a-button class="proto-button acid-button" type="primary" @click="router.push('/prototype/gallery/upload')">上传第一张图片</a-button>
          </template>
        </a-empty>
      </div>
    </a-spin>

    <div v-if="total > 0" class="gallery-pagination">
      <span class="gallery-pagination-caption">第 {{ current }} 页</span>
      <a-pagination
        v-model:current="current"
        v-model:pageSize="pageSize"
        :total="total"
        :page-size-options="['6', '10']"
        show-size-changer
        show-less-items
        :show-total="(value: number) => `共 ${value} 张`"
        @change="handlePageChange"
        @show-size-change="handlePageSizeChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { queryPicturePageCacheUsingPost, listPictureCategoryUsingGet } from '../../../api/pictureController'

const router = useRouter()
const loading = ref(false)
const loadError = ref('')
const pictureList = ref<API.PictureVO[]>([])
const total = ref(0)
const current = ref(1)
const pageSize = ref(10)
const searchText = ref('')
const category = ref('')
const selectedTags = ref<string[]>([])
const categories = ref<string[]>([])
const tags = ref<string[]>([])
const sortOrder = ref<'latest' | 'size'>('latest')

async function loadGallery() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await queryPicturePageCacheUsingPost({
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
    const res = await listPictureCategoryUsingGet()
    if (res.data?.code === 200) {
      categories.value = res.data.data?.categorys || []
      tags.value = res.data.data?.tags || []
    }
  } catch {
    message.warning('分类和标签暂时加载失败，仍可使用关键词查询')
  }
}

function runSearch() {
  current.value = 1
  void loadGallery()
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
  current.value = 1
  void loadGallery()
}

function handlePageChange(page: number, size: number) {
  current.value = page
  pageSize.value = Math.min(size || pageSize.value, 10)
  void loadGallery()
}

function handlePageSizeChange(page: number, size: number) {
  current.value = page
  pageSize.value = Math.min(size || pageSize.value, 10)
  void loadGallery()
}

function openDetail(id?: number | string) {
  const normalizedId = String(id || '').trim()
  if (normalizedId) router.push(`/prototype/gallery/detail/${encodeURIComponent(normalizedId)}`)
}

onMounted(() => {
  void Promise.all([loadCategories(), loadGallery()])
})
</script>

<style scoped>
.gallery-prototype { margin-inline: -26px; padding-top: 2px; }
/* 缩短图库首屏头部，让用户更快看到图片内容。 */
.gallery-hero { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 20px; align-items: end; padding: 12px 0 14px; border-bottom: 1px solid var(--proto-line); }
.gallery-hero-copy { max-width: 960px; }
.gallery-brand { margin-bottom: 4px; color: var(--proto-muted); font-family: 'Geist', 'Manrope', Arial, sans-serif; font-size: clamp(30px, 3.2vw, 48px); font-weight: 800; letter-spacing: -.055em; line-height: .95; }
.gallery-title { max-width: 960px; margin: 0; color: var(--proto-ink); font-family: 'Cabinet Grotesk', 'Geist', 'Manrope', Arial, sans-serif; font-size: clamp(25px, 2.6vw, 38px); font-weight: 700; line-height: 1.05; letter-spacing: -.04em; text-wrap: balance; }
.gallery-hero-actions { display: flex; align-items: center; justify-content: flex-end; gap: 10px; padding-bottom: 3px; }
.gallery-hero-actions .ant-btn { min-width: 124px; height: 44px; border-radius: 8px; font-family: 'Geist', 'Manrope', Arial, sans-serif; font-size: 14px; font-weight: 700; letter-spacing: -.01em; }
.gallery-hero-actions .ghost-button.ant-btn { border-width: 2px; background: var(--proto-paper); }
.gallery-hero-actions .acid-button.ant-btn-primary { box-shadow: 0 8px 18px rgba(186, 255, 61, .28); }
.gallery-search { padding-top: 12px; }
.gallery-filter-card.ant-card { overflow: hidden; border-radius: 13px; background: rgba(255, 255, 255, .58); }
.gallery-filter-card :deep(.ant-card-body) { padding: 0; }
.gallery-filter-row { display: grid; grid-template-columns: minmax(0, 1fr) 180px auto; gap: 10px; align-items: center; padding: 10px 14px; }
.gallery-search-input { min-width: 0; }
.gallery-search-input :deep(.ant-input),
.gallery-search-input :deep(.ant-input-search-button),
.gallery-category-select :deep(.ant-select-selector) { height: 42px !important; }
.gallery-search-input :deep(.ant-input),
.gallery-category-select :deep(.ant-select-selector) { border-color: rgba(17, 20, 22, .16) !important; background: rgba(246, 247, 242, .86) !important; box-shadow: none !important; }
.gallery-search-input :deep(.ant-input) { border-radius: 9px 0 0 9px !important; padding-inline: 14px; }
.gallery-search-input :deep(.ant-input-search-button) { border: 1px solid var(--proto-ink) !important; border-radius: 0 9px 9px 0 !important; background: var(--proto-ink); color: var(--proto-paper); font-weight: 700; }
.gallery-search-input :deep(.ant-input-search-button:hover) { background: var(--proto-orange); border-color: var(--proto-orange) !important; color: var(--proto-ink); }
.gallery-search-input :deep(.ant-input),
.gallery-search-input :deep(.ant-input-search-button),
.gallery-category-select :deep(.ant-select-selection-item),
.gallery-category-select :deep(.ant-select-selection-placeholder) { font-family: 'Geist', 'Manrope', Arial, sans-serif; font-size: 14px; }
.gallery-category-select { width: 100%; }
.gallery-category-select :deep(.ant-select-selector) { display: flex !important; align-items: center; border-radius: 9px !important; padding-inline: 13px !important; }
.gallery-reset-button.ant-btn { height: 42px; border-radius: 9px; font-family: 'Geist', 'Manrope', Arial, sans-serif; font-size: 14px; font-weight: 700; }
.gallery-category-select :deep(.ant-select-selection-item),
.gallery-category-select :deep(.ant-select-selection-placeholder) { display: inline-flex; align-items: center; line-height: 1.2; }
.gallery-tag-row { display: flex; min-height: 45px; align-items: center; flex-wrap: wrap; gap: 7px; padding: 8px 14px 9px; border-top: 1px solid var(--proto-line); }
.gallery-muted { color: var(--proto-muted); font-size: 12px; }
.gallery-tag-row :deep(.ant-tag-checkable) { margin: 0; padding: 6px 11px; border: 1px solid rgba(17, 20, 22, .14); border-radius: 7px; background: transparent; color: var(--proto-muted); font-family: 'Geist', 'Manrope', Arial, sans-serif; font-size: 13px; line-height: 1.35; transition: background .25s ease, border-color .25s ease, color .25s ease; }
.gallery-tag-row :deep(.ant-tag-checkable:hover) { border-color: var(--proto-ink); color: var(--proto-ink); }
.gallery-tag-row :deep(.ant-tag-checkable-checked) { border-color: var(--proto-acid); background: var(--proto-acid); color: var(--proto-ink); }
.gallery-results-bar { display: flex; align-items: center; justify-content: space-between; gap: 18px; padding: 14px 0 10px; }
.gallery-result-summary { display: flex; align-items: baseline; gap: 10px; }
.result-count { color: var(--proto-ink); font-family: 'Cabinet Grotesk', 'Geist', 'Manrope', Arial, sans-serif; font-size: 39px; font-weight: 800; letter-spacing: -.08em; line-height: 1; }
.gallery-result-label { color: var(--proto-muted); font-family: 'Geist', 'Manrope', Arial, sans-serif; font-size: 16px; font-weight: 600; }
.gallery-sort-group :deep(.ant-radio-button-wrapper) { height: 35px; border-color: var(--proto-line); background: transparent; color: var(--proto-muted); font-size: 12px; line-height: 33px; }
.gallery-sort-group :deep(.ant-radio-button-wrapper:hover) { color: var(--proto-ink); }
.gallery-sort-group :deep(.ant-radio-button-wrapper-checked) { border-color: var(--proto-ink); background: var(--proto-ink); color: var(--proto-paper); }
.gallery-alert { margin-bottom: 14px; }
.gallery-grid { display: grid; grid-auto-flow: dense; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 12px; }
.gallery-card.ant-card { position: relative; display: block; overflow: hidden; cursor: pointer; border-radius: 12px; background: var(--proto-paper-deep); transition: transform .4s cubic-bezier(.22, 1, .36, 1), box-shadow .4s ease, border-color .3s ease; }
.gallery-card :deep(.ant-card-body) { padding: 0; }
.gallery-card:hover { border-color: rgba(17, 20, 22, .35); transform: translateY(-5px); box-shadow: var(--proto-shadow); }
.gallery-card:focus-visible { border-color: var(--proto-ink); box-shadow: 0 0 0 3px rgba(186, 255, 61, .35); outline: none; }
.gallery-card-image { position: relative; aspect-ratio: 5 / 6; overflow: hidden; background: var(--proto-paper-deep); }
.gallery-card-image::after { position: absolute; inset: 0; background: linear-gradient(180deg, rgba(17, 20, 22, 0) 38%, rgba(17, 20, 22, .86) 100%); content: ''; opacity: 0; pointer-events: none; transition: opacity .25s ease; }
.gallery-card-image img { display: block; width: 100%; height: 100%; object-fit: cover; }
.gallery-card-overlay { position: absolute; z-index: 3; top: 12px; right: 12px; bottom: auto; padding: 6px 9px; border-radius: 6px; background: rgba(246, 247, 242, .92); color: var(--proto-ink); font-size: 11px; font-weight: 700; opacity: 0; pointer-events: none; transform: translateY(-5px); transition: opacity .25s ease, transform .25s ease; }
.gallery-card:hover .gallery-card-overlay,
.gallery-card:focus-visible .gallery-card-overlay,
.gallery-card:hover .gallery-card-image::after,
.gallery-card:focus-visible .gallery-card-image::after { opacity: 1; transform: translateY(0); }
.gallery-image-empty { display: grid; height: 100%; place-items: center; color: var(--proto-muted); font-size: 12px; }
.gallery-card-body { position: absolute; z-index: 2; right: 0; bottom: 0; left: 0; padding: 42px 14px 14px; opacity: 0; pointer-events: none; transform: translateY(8px); transition: opacity .25s ease, transform .25s ease; }
.gallery-card:hover .gallery-card-body,
.gallery-card:focus-visible .gallery-card-body { opacity: 1; transform: translateY(0); }
.gallery-card-title { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.gallery-card-title strong { overflow: hidden; color: var(--proto-paper); font-size: 15px; font-weight: 800; letter-spacing: -.045em; text-overflow: ellipsis; white-space: nowrap; }
.gallery-card-arrow { color: var(--proto-orange); font-family: 'DM Mono', monospace; font-size: 17px; line-height: 1; transition: transform .3s ease; }
.gallery-card:hover .gallery-card-arrow { transform: translate(3px, -3px); }
.gallery-card-body p { display: -webkit-box; min-height: 0; margin: 7px 0 11px; overflow: hidden; color: rgba(246, 247, 242, .82); font-size: 11px; line-height: 1.55; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.gallery-card-foot { display: flex; align-items: center; justify-content: space-between; gap: 8px; color: rgba(246, 247, 242, .78); font-size: 10px; }
.gallery-category-tag.ant-tag { margin: 0; border: 0; border-radius: 5px; background: rgba(246, 247, 242, .9); color: var(--proto-ink); font-size: 10px; line-height: 20px; }
.gallery-author { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.gallery-empty-state { padding: 46px 24px; border: 1px solid var(--proto-line); border-radius: 12px; background: rgba(255, 255, 255, .45); }
.gallery-empty-state :deep(.ant-empty-description) { color: var(--proto-muted); }
.gallery-pagination { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 18px 0 4px; border-top: 1px solid var(--proto-line); }
.gallery-pagination-caption { color: var(--proto-muted); font-family: 'DM Mono', monospace; font-size: 11px; }
.gallery-pagination :deep(.ant-pagination) { margin: 0; }
.gallery-pagination :deep(.ant-pagination-item-active) { border-color: var(--proto-ink); background: var(--proto-ink); }
.gallery-pagination :deep(.ant-pagination-item-active a) { color: var(--proto-paper); }
@media (prefers-reduced-motion: reduce) {
  .gallery-card.ant-card,
  .gallery-card-image::after,
  .gallery-card-overlay,
  .gallery-card-body,
  .gallery-card-arrow { transition: none; }
}
@media (max-width: 1440px) {
  .gallery-grid { grid-template-columns: repeat(4, minmax(0, 1fr)); }
}
@media (max-width: 1100px) {
  .gallery-prototype { margin-inline: -10px; }
  .gallery-hero { grid-template-columns: 1fr; gap: 18px; }
  .gallery-hero-actions { justify-content: flex-start; }
  .gallery-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 760px) {
  .gallery-prototype { margin-inline: 0; }
  .gallery-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .gallery-filter-row { grid-template-columns: 1fr; }
  .gallery-reset-button.ant-btn { width: 100%; }
  .gallery-results-bar { align-items: flex-start; flex-direction: column; gap: 12px; }
  .gallery-pagination { align-items: flex-start; flex-direction: column; }
}
@media (max-width: 570px) {
  .gallery-hero { padding-top: 14px; }
  .gallery-brand { font-size: clamp(29px, 10vw, 42px); }
  .gallery-title { font-size: clamp(24px, 7vw, 32px); }
  .gallery-hero-actions { width: 100%; }
  .gallery-hero-actions .ant-btn { flex: 1; }
  .gallery-grid { grid-template-columns: 1fr; }
}
</style>
