<template>
  <article
    class="gallery-card proto-surface"
    :class="{ 'is-list': viewMode === 'list', 'is-manage': showManageControls }"
    role="button"
    tabindex="0"
    @click="emit('open')"
    @keydown.enter="emit('open')"
    @keydown.space.prevent="emit('open')"
  >
    <div class="gallery-card-image">
      <img
        v-if="picture.thumbnailUrl || picture.url"
        :src="picture.thumbnailUrl || picture.url"
        :alt="picture.name || '图片'"
        loading="lazy"
      />
      <div v-else class="gallery-image-empty">暂无图片地址</div>

      <div
        v-if="showManageControls"
        class="gallery-card-select"
        @click.stop
      >
        <a-checkbox
          :checked="selected"
          :aria-label="`选择 ${picture.name || '图片'}`"
          @change="handleSelectChange"
        />
      </div>

      <a-tag
        v-if="showManageControls && statusText"
        class="gallery-card-status"
        :class="statusClass"
      >
        {{ statusText }}
      </a-tag>

      <!-- 公共图库保留收藏/下载的统一视觉位置；接口接通后再补真实动作。 -->
      <div v-if="!showManageControls" class="gallery-card-tools" aria-hidden="true">
        <span class="gallery-card-tool"><HeartOutlined /></span>
        <span class="gallery-card-tool"><DownloadOutlined /></span>
      </div>

      <a-dropdown v-if="showManageControls" placement="bottomRight" :trigger="['click']">
        <button
          type="button"
          class="gallery-card-menu"
          :aria-label="`打开 ${picture.name || '图片'} 操作菜单`"
          @click.stop
        >
          <MoreOutlined />
        </button>
        <template #overlay>
          <a-menu @click="handleMenuClick">
            <a-menu-item key="detail">查看详情</a-menu-item>
            <a-menu-item key="edit">编辑信息</a-menu-item>
            <a-menu-item v-if="reuploadEnabled" key="reupload">重新上传</a-menu-item>
            <a-menu-divider />
            <a-menu-item key="delete" danger>删除图片</a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>

      <span class="gallery-card-overlay">查看详情</span>

      <div class="gallery-card-meta">
        <div class="gallery-card-author">
          <a-avatar :size="22" :src="picture.createdUser?.avatarurl">
            {{ authorFallback }}
          </a-avatar>
          <span>{{ picture.createdUser?.username || '未知用户' }}</span>
          <CheckCircleFilled class="gallery-author-verified" aria-hidden="true" />
        </div>
        <span class="gallery-card-size">{{ pictureDimensions }}</span>
      </div>
    </div>

    <slot name="body">
      <div v-if="viewMode === 'list'" class="gallery-card-list-copy">
        <div class="gallery-card-list-head">
          <strong>{{ picture.name || '未命名图片' }}</strong>
          <span>{{ picture.category || '未分类' }}</span>
        </div>
        <p>{{ picture.introduction || '暂无图片简介' }}</p>
      </div>
    </slot>
  </article>
</template>

<script setup lang="ts">
import {
  CheckCircleFilled,
  DownloadOutlined,
  HeartOutlined,
  MoreOutlined,
} from '@ant-design/icons-vue'
import { computed } from 'vue'

type ViewMode = 'grid' | 'list'

const props = withDefaults(
  defineProps<{
    picture: API.PictureVO
    viewMode?: ViewMode
    showManageControls?: boolean
    selected?: boolean
    statusText?: string
    statusClass?: string
    reuploadEnabled?: boolean
  }>(),
  {
    viewMode: 'grid',
    showManageControls: false,
    selected: false,
    statusText: '',
    statusClass: '',
    reuploadEnabled: false,
  },
)

const emit = defineEmits<{
  (event: 'open'): void
  (event: 'select', checked: boolean): void
  (event: 'menu', key: string): void
}>()

const authorFallback = computed(() =>
  (props.picture.createdUser?.username || '用').charAt(0),
)

const pictureDimensions = computed(() => {
  if (props.picture.picwidth && props.picture.picheight) {
    return `${props.picture.picwidth} × ${props.picture.picheight}`
  }
  return '尺寸未知'
})

function handleSelectChange(event: any) {
  emit('select', Boolean(event.target.checked))
}

function handleMenuClick(event: { key: string }) {
  emit('menu', String(event.key))
}
</script>

<style scoped>
.gallery-card {
  position: relative;
  min-width: 0;
  overflow: hidden;
  border: 1px solid rgba(17, 20, 22, .07);
  border-radius: 11px;
  background: var(--proto-paper-deep);
  cursor: pointer;
  transition: box-shadow .25s ease, border-color .2s ease;
}

.gallery-card:hover {
  border-color: rgba(17, 20, 22, .25);
  box-shadow: var(--proto-shadow);
}

.gallery-card:focus-visible {
  border-color: var(--proto-ink);
  box-shadow: 0 0 0 3px rgba(186, 255, 61, .35);
  outline: none;
}

.gallery-card-image {
  position: relative;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background: var(--proto-paper-deep);
}

.gallery-card-image::after {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(17, 20, 22, 0) 43%, rgba(17, 20, 22, .8) 100%);
  content: '';
  pointer-events: none;
}

.gallery-card-image img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.gallery-card-tools {
  position: absolute;
  z-index: 3;
  top: 10px;
  right: 10px;
  display: flex;
  gap: 6px;
}

.gallery-card-tool {
  display: grid;
  width: 27px;
  height: 27px;
  place-items: center;
  border: 1px solid rgba(17, 20, 22, .07);
  border-radius: 50%;
  background: rgba(246, 247, 242, .9);
  color: var(--proto-ink-soft);
  font-size: 14px;
}

.gallery-card-overlay {
  position: absolute;
  z-index: 4;
  top: 10px;
  left: 10px;
  padding: 5px 7px;
  border-radius: 5px;
  background: rgba(246, 247, 242, .9);
  color: var(--proto-ink);
  font-size: 10px;
  font-weight: 700;
  opacity: 0;
  transform: translateY(-4px);
  transition: opacity .2s ease, transform .2s ease;
}

.gallery-card:hover .gallery-card-overlay,
.gallery-card:focus-visible .gallery-card-overlay {
  opacity: 1;
  transform: translateY(0);
}

.gallery-card-meta {
  position: absolute;
  z-index: 3;
  right: 11px;
  bottom: 10px;
  left: 11px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: var(--proto-paper);
  font-size: 10px;
}

.gallery-card-author {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 5px;
}

.gallery-card-author :deep(.ant-avatar) {
  flex: 0 0 auto;
  border: 1px solid rgba(246, 247, 242, .8);
  background: var(--proto-paper-deep);
  color: var(--proto-ink);
  font-size: 10px;
}

.gallery-card-author > span {
  overflow: hidden;
  max-width: 100px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gallery-author-verified {
  flex: 0 0 auto;
  color: #68a8ff;
  font-size: 11px;
}

.gallery-card-size {
  flex: 0 0 auto;
  color: rgba(246, 247, 242, .85);
  font-family: 'DM Mono', monospace;
  font-size: 9px;
}

.gallery-image-empty {
  display: grid;
  height: 100%;
  place-items: center;
  color: var(--proto-muted);
  font-size: 12px;
}

.gallery-card-select,
.gallery-card-status,
.gallery-card-menu {
  position: absolute;
  z-index: 5;
}

.gallery-card-select {
  top: 10px;
  left: 10px;
  display: inline-flex;
  width: 27px;
  height: 27px;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: rgba(246, 247, 242, .92);
}

.gallery-card-status {
  top: 11px;
  left: 43px;
  margin: 0;
  padding: 3px 8px;
  border: 0;
  border-radius: 999px;
  background: rgba(186, 255, 61, .9);
  color: var(--proto-ink);
  font-size: 10px;
  font-weight: 700;
  line-height: 1.2;
}

.gallery-card-status.wait {
  background: #ffc34d;
}

.gallery-card-status.refuse {
  background: #ff987d;
}

.gallery-card-menu {
  top: 9px;
  right: 9px;
  display: inline-flex;
  width: 30px;
  height: 30px;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  border-radius: 999px;
  background: rgba(246, 247, 242, .92);
  color: var(--proto-ink);
  cursor: pointer;
  font-size: 16px;
}

.gallery-card-menu:hover {
  background: var(--proto-acid);
}

.gallery-card-list-copy {
  min-width: 0;
  padding: 15px 16px 16px;
  background: rgba(255, 255, 255, .72);
}

.gallery-card-list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.gallery-card-list-head strong {
  overflow: hidden;
  color: var(--proto-ink);
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gallery-card-list-head span {
  flex: 0 0 auto;
  color: var(--proto-muted);
  font-size: 11px;
}

.gallery-card-list-copy p {
  display: -webkit-box;
  margin: 8px 0 0;
  overflow: hidden;
  color: var(--proto-muted);
  font-size: 11px;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.gallery-card.is-list {
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr);
}

.gallery-card.is-list .gallery-card-image {
  aspect-ratio: auto;
  min-height: 170px;
}

.gallery-card.is-list .gallery-card-list-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

@media (max-width: 600px) {
  .gallery-card.is-list {
    grid-template-columns: 150px minmax(0, 1fr);
  }

  .gallery-card.is-list .gallery-card-image {
    min-height: 120px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .gallery-card,
  .gallery-card-overlay {
    transition: none;
  }
}
</style>
