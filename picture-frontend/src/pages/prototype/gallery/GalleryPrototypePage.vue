<template>
  <div class="gallery-prototype">
    <section class="proto-page-head">
      <div>
        <span class="proto-eyebrow">公共图库 / queryPicturePageCache</span>
        <h1 class="proto-title">找到一张<br />值得留下的图片。</h1>
        <p class="proto-copy">按关键词、分类和标签浏览审核通过的公共图片。查询参数严格对应后端的 searchText、category、tags、current、pageSize、spaceId 和 sortFiled。</p>
      </div>
      <div class="proto-page-head-actions">
        <a-button class="proto-button ghost-button" @click="router.push('/prototype/gallery/manage')">管理我的图片</a-button>
        <a-button class="proto-button acid-button" type="primary" @click="router.push('/prototype/gallery/upload')">上传图片</a-button>
      </div>
    </section>

    <section class="gallery-search proto-section">
      <div class="proto-surface proto-rounded">
        <div class="proto-toolbar">
          <a-input-search v-model:value="searchText" placeholder="搜索图片名称或简介" enter-button="搜索" @search="runSearch" />
          <a-select v-model:value="category" style="width: 150px" placeholder="图片分类" @change="runSearch">
            <a-select-option value="">全部分类</a-select-option>
            <a-select-option v-for="item in categories" :key="item" :value="item">{{ item }}</a-select-option>
          </a-select>
          <a-button class="proto-button ghost-button" @click="resetFilters">清空筛选</a-button>
        </div>
        <div class="gallery-tag-row">
          <span class="filter-caption">tags</span>
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
      </div>
    </section>

    <section class="gallery-result-head">
      <div class="proto-flex proto-gap-12">
        <span class="result-count">{{ total }}</span>
        <span class="proto-muted">张公开图片</span>
      </div>
      <a-radio-group v-model:value="sortOrder" button-style="solid" size="small" @change="runSearch">
        <a-radio-button value="latest">最近上传</a-radio-button>
        <a-radio-button value="size">尺寸优先</a-radio-button>
      </a-radio-group>
    </section>

    <a-alert v-if="loadError" class="gallery-alert" type="error" show-icon :message="loadError" />

    <a-spin :spinning="loading" tip="加载图片中...">
      <section v-if="pictureList.length" class="gallery-grid" aria-label="公共图片列表">
        <article
          v-for="picture in pictureList"
          :key="picture.id"
          class="gallery-card"
          @click="openDetail(picture.id)"
        >
          <div class="gallery-card-image proto-image-wrap">
            <img v-if="picture.thumbnailUrl || picture.url" :src="picture.thumbnailUrl || picture.url" :alt="picture.name || '公共图片'" loading="lazy" />
            <div v-else class="gallery-image-empty">暂无图片地址</div>
          </div>
          <div class="gallery-card-body">
            <div class="gallery-card-title">
              <strong>{{ picture.name || '未命名图片' }}</strong>
              <span class="proto-mono">#{{ picture.id }}</span>
            </div>
            <p>{{ picture.introduction || '暂无图片简介' }}</p>
            <div class="gallery-card-foot">
              <span>{{ picture.category || '未分类' }}</span>
              <span>{{ picture.createdUser?.username || '未知用户' }}</span>
            </div>
          </div>
        </article>
      </section>
      <a-empty v-else-if="!loading" description="没有符合当前筛选条件的图片" />
    </a-spin>

    <div v-if="total > 0" class="gallery-pagination">
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
const pageSize = ref(6)
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
    total.value = res.data.data?.total || 0
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
.gallery-search { padding-top: 22px; }
.gallery-tag-row { min-height: 59px; padding: 13px 18px 9px; display: flex; align-items: center; flex-wrap: wrap; gap: 5px; }
.filter-caption { margin-right: 8px; color: var(--proto-orange); font-family: 'DM Mono', monospace; font-size: 11px; }
.gallery-muted { color: var(--proto-muted); font-size: 11px; }
.gallery-tag-row :deep(.ant-tag-checkable) { margin: 0; padding: 5px 9px; border-radius: 3px; color: var(--proto-muted); font-size: 11px; }
.gallery-tag-row :deep(.ant-tag-checkable-checked) { background: var(--proto-acid); color: var(--proto-ink); }
.gallery-result-head { padding: 24px 0 14px; display: flex; align-items: center; justify-content: space-between; }
.result-count { font-size: 33px; font-weight: 800; letter-spacing: -.08em; }
.gallery-result-head :deep(.ant-radio-button-wrapper) { border-color: var(--proto-line); background: transparent; font-size: 11px; }
.gallery-result-head :deep(.ant-radio-button-wrapper-checked) { border-color: var(--proto-ink); background: var(--proto-ink); }
.gallery-alert { margin-bottom: 14px; }
.gallery-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.gallery-card { overflow: hidden; cursor: pointer; background: rgba(255,255,255,.55); border: 1px solid var(--proto-line); transition: transform .35s ease, box-shadow .35s ease; }
.gallery-card:hover { transform: translateY(-6px); box-shadow: var(--proto-shadow); }
.gallery-card-image { height: 245px; }
.gallery-card-image img { display: block; width: 100%; height: 100%; object-fit: cover; }
.gallery-image-empty { height: 100%; display: grid; place-items: center; color: var(--proto-muted); font-size: 11px; }
.gallery-card-body { padding: 16px 16px 14px; }
.gallery-card-title { display: flex; align-items: center; justify-content: space-between; gap: 9px; }
.gallery-card-title strong { font-size: 16px; letter-spacing: -.04em; }
.gallery-card-title span { color: var(--proto-muted); font-size: 10px; }
.gallery-card-body p { min-height: 39px; margin: 9px 0 14px; color: var(--proto-muted); font-size: 11px; line-height: 1.65; }
.gallery-card-foot { display: flex; justify-content: space-between; color: var(--proto-muted); font-size: 10px; }
.gallery-pagination { display: flex; justify-content: flex-end; padding-top: 16px; }
.gallery-pagination :deep(.ant-pagination-item-active) { border-color: var(--proto-ink); background: var(--proto-ink); }
.gallery-pagination :deep(.ant-pagination-item-active a) { color: var(--proto-paper); }
@media (max-width: 900px) { .gallery-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 570px) { .gallery-grid { grid-template-columns: 1fr; } .gallery-result-head { align-items: flex-start; flex-direction: column; gap: 15px; } .gallery-card-image { height: 260px; } }
</style>
