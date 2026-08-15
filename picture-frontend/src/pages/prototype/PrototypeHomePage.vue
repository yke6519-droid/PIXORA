<template>
  <div class="prototype-home">
    <section class="home-hero" aria-labelledby="home-title">
      <div class="home-hero-copy">
        <div class="home-brandline">
          <span class="home-brand-mark">P</span>
          <strong>PIXORA</strong>
          <span class="home-brand-rule" aria-hidden="true"></span>
          <span class="home-brand-context">{{ userContext }}</span>
        </div>

        <h1 id="home-title">图片，从这里开始。</h1>
        <p class="home-hero-description">
          浏览公共图库，上传新的图片，或继续整理已经属于你的内容。
        </p>

        <div class="home-hero-actions">
          <a-button class="proto-button acid-button" type="primary" @click="router.push('/prototype/gallery')">
            浏览公共图库
          </a-button>
          <a-button class="proto-button hero-secondary-button" @click="router.push('/prototype/gallery/upload')">
            上传图片
          </a-button>
        </div>

        <div class="home-hero-footer">
          <span>公共图库</span>
          <strong>{{ total }} 张已审核图片</strong>
          <span class="home-hero-footer-arrow" aria-hidden="true">↗</span>
        </div>
      </div>

      <div class="home-hero-gallery" aria-label="公共图库最近上传">
        <template v-if="pictureList.length">
          <button
            class="home-feature home-feature-large"
            type="button"
            @click="openDetail(pictureList[0]?.id)"
          >
            <a-image
              :src="pictureImage(pictureList[0])"
              :preview="false"
              :alt="pictureList[0]?.name || '公共图库图片'"
            />
            <span class="home-feature-overlay">
              <strong>{{ pictureList[0]?.name || '未命名图片' }}</strong>
              <span>查看图片详情 ↗</span>
            </span>
          </button>

          <div class="home-feature-stack">
            <button
              v-for="picture in pictureList.slice(1, 3)"
              :key="picture.id"
              class="home-feature home-feature-small"
              type="button"
              @click="openDetail(picture.id)"
            >
              <a-image
                :src="pictureImage(picture)"
                :preview="false"
                :alt="picture.name || '公共图库图片'"
              />
              <span class="home-feature-overlay">
                <strong>{{ picture.name || '未命名图片' }}</strong>
                <span>打开 ↗</span>
              </span>
            </button>
          </div>
        </template>

        <a-skeleton v-else-if="loading" class="home-gallery-skeleton" active :paragraph="false" />
        <a-empty v-else class="home-gallery-empty" description="暂时没有可展示的图片" />
      </div>
    </section>

    <section class="home-library" aria-labelledby="home-library-title">
      <div class="home-section-heading">
        <div>
          <span class="home-section-kicker">最近上传</span>
          <h2 id="home-library-title">公共图库</h2>
        </div>
        <RouterLink to="/prototype/gallery" class="home-section-link">查看全部 <span aria-hidden="true">↗</span></RouterLink>
      </div>

      <a-alert v-if="loadError" class="home-alert" type="error" show-icon :message="loadError" />

      <div v-if="pictureList.length" class="home-library-grid">
        <button
          v-for="picture in pictureList.slice(0, 5)"
          :key="picture.id"
          class="home-library-item"
          type="button"
          @click="openDetail(picture.id)"
        >
          <a-image
            :src="pictureImage(picture)"
            :preview="false"
            :alt="picture.name || '公共图库图片'"
          />
          <span class="home-library-overlay">
            <strong>{{ picture.name || '未命名图片' }}</strong>
            <span>{{ picture.category || '未分类' }} · {{ picture.createdUser?.username || '未知用户' }}</span>
          </span>
        </button>
      </div>

      <a-empty v-else-if="!loading && !loadError" description="公共图库暂时没有图片" />
    </section>

    <section class="home-continue" aria-labelledby="home-continue-title">
      <div class="home-section-heading home-continue-heading">
        <div>
          <span class="home-section-kicker">继续使用</span>
          <h2 id="home-continue-title">进入你的工作区</h2>
        </div>
      </div>

      <div class="home-continue-grid">
        <RouterLink to="/prototype/gallery/manage" class="home-continue-item home-continue-dark">
          <span class="home-continue-label">整理</span>
          <strong>管理我的图片</strong>
          <span class="home-continue-arrow" aria-hidden="true">↗</span>
        </RouterLink>
        <RouterLink to="/prototype/user/center" class="home-continue-item home-continue-acid">
          <span class="home-continue-label">账户</span>
          <strong>打开用户中心</strong>
          <span class="home-continue-arrow" aria-hidden="true">↗</span>
        </RouterLink>
        <RouterLink to="/prototype/space" class="home-continue-item home-continue-paper">
          <span class="home-continue-label">收纳</span>
          <strong>进入个人空间</strong>
          <span class="home-continue-arrow" aria-hidden="true">↗</span>
        </RouterLink>
      </div>
    </section>

    <section v-if="isAdmin" class="home-admin" aria-labelledby="home-admin-title">
      <div class="home-section-heading home-continue-heading">
        <div>
          <span class="home-section-kicker">管理员</span>
          <h2 id="home-admin-title">平台管理</h2>
        </div>
      </div>

      <div class="home-admin-list">
        <RouterLink to="/prototype/admin/pictures/review" class="home-admin-link">
          <span>图片审核</span>
          <small>处理待审核图片</small>
          <strong aria-hidden="true">↗</strong>
        </RouterLink>
        <RouterLink to="/prototype/admin/pictures/import" class="home-admin-link">
          <span>批量抓图</span>
          <small>批量拉取并上传图片</small>
          <strong aria-hidden="true">↗</strong>
        </RouterLink>
        <RouterLink to="/prototype/admin/users" class="home-admin-link">
          <span>用户管理</span>
          <small>查看平台用户</small>
          <strong aria-hidden="true">↗</strong>
        </RouterLink>
        <RouterLink to="/prototype/admin/spaces" class="home-admin-link">
          <span>空间运营</span>
          <small>查看空间使用情况</small>
          <strong aria-hidden="true">↗</strong>
        </RouterLink>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { queryPicturePageCache } from '../../api/pictureController'
import { useLoginUserStore } from '../../stores/useLoginUserStore'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const loading = ref(false)
const loadError = ref('')
const pictureList = ref<API.PictureVO[]>([])
const total = ref(0)

// 管理入口只对当前登录的管理员展示，普通用户和匿名访客不会看到后台功能。
const isAdmin = computed(() => loginUserStore.loginUser?.userLevel === 'admin')
const userContext = computed(() => {
  const username = loginUserStore.loginUser?.username
  return username ? `你好，${username}` : '开放浏览'
})

async function loadPublicPictures() {
  loading.value = true
  loadError.value = ''
  try {
    // 首页只读取公共图库，并显式传 0，避免把 null 传给后端权限分支。
    const res = await queryPicturePageCache({
      current: 1,
      pageSize: 5,
      pictureCheck: 1,
      spaceId: 0,
      sortFiled: 'createtime',
      sortOrder: 'descend',
    })
    if (res.data?.code !== 200) {
      throw new Error(res.data?.message || '公共图库加载失败')
    }
    pictureList.value = res.data.data?.pictureList || []
    total.value = Number(res.data.data?.total || 0)
  } catch (error: any) {
    pictureList.value = []
    total.value = 0
    loadError.value = error?.response?.data?.message || error?.message || '公共图库暂时无法加载'
  } finally {
    loading.value = false
  }
}

function pictureImage(picture?: API.PictureVO) {
  return picture?.thumbnailUrl || picture?.url || ''
}

function openDetail(id?: number | string) {
  const normalizedId = String(id || '').trim()
  if (normalizedId) {
    router.push(`/prototype/gallery/detail/${encodeURIComponent(normalizedId)}`)
  }
}

onMounted(() => {
  void loadPublicPictures()
  if (!loginUserStore.loginUser) {
    void loginUserStore.fetchLoginUser()
  }
})
</script>

<style scoped>
.prototype-home {
  max-width: 1280px;
  margin: 0 auto;
  padding: 22px 0 42px;
}

.home-hero {
  min-height: 420px;
  display: grid;
  grid-template-columns: minmax(340px, .86fr) minmax(0, 1.14fr);
  overflow: hidden;
  border-radius: 12px;
  background: var(--proto-ink);
  color: var(--proto-paper);
  box-shadow: var(--proto-shadow);
}

.home-hero-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding: 34px 38px 26px;
}

.home-brandline {
  display: flex;
  align-items: center;
  gap: 9px;
  color: var(--proto-paper);
  font-size: 13px;
  letter-spacing: .08em;
}

.home-brand-mark {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--proto-acid);
  color: var(--proto-ink);
  font-size: 15px;
  font-weight: 800;
  letter-spacing: 0;
}

.home-brand-rule {
  width: 30px;
  height: 1px;
  margin-inline: 4px;
  background: rgba(241, 242, 237, .34);
}

.home-brand-context {
  color: rgba(241, 242, 237, .62);
  font-size: 11px;
  letter-spacing: 0;
}

.home-hero h1 {
  max-width: 440px;
  margin: auto 0 13px;
  color: var(--proto-paper);
  font-size: clamp(38px, 4vw, 58px);
  line-height: .98;
  letter-spacing: -.07em;
  font-weight: 800;
  text-wrap: balance;
}

.home-hero-description {
  max-width: 31ch;
  margin: 0;
  color: rgba(241, 242, 237, .72);
  font-size: 14px;
  line-height: 1.65;
}

.home-hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
  margin-top: 25px;
}

.home-hero-actions .ant-btn {
  min-width: 122px;
  height: 42px;
  border-radius: 6px;
  font-size: 13px;
}

.home-hero-actions .hero-secondary-button {
  border-color: rgba(241, 242, 237, .66);
  background: transparent;
  color: var(--proto-paper);
}

.home-hero-actions .hero-secondary-button:hover {
  border-color: var(--proto-acid) !important;
  color: var(--proto-acid) !important;
}

.home-hero-footer {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-top: auto;
  padding-top: 25px;
  color: rgba(241, 242, 237, .54);
  font-size: 11px;
}

.home-hero-footer strong {
  color: var(--proto-paper);
  font-weight: 600;
}

.home-hero-footer-arrow {
  margin-left: auto;
  color: var(--proto-acid);
  font-size: 17px;
}

.home-hero-gallery {
  min-width: 0;
  display: grid;
  grid-template-columns: minmax(0, 1.46fr) minmax(150px, .7fr);
  gap: 9px;
  padding: 9px;
  background: var(--proto-paper-deep);
}

.home-feature,
.home-library-item {
  position: relative;
  min-width: 0;
  padding: 0;
  overflow: hidden;
  border: 0;
  cursor: pointer;
  text-align: left;
}

.home-feature {
  background: var(--proto-paper);
}

.home-feature :deep(.ant-image),
.home-feature :deep(.ant-image-img),
.home-library-item :deep(.ant-image),
.home-library-item :deep(.ant-image-img) {
  display: block;
  width: 100%;
  height: 100%;
}

.home-feature :deep(.ant-image-img),
.home-library-item :deep(.ant-image-img) {
  object-fit: cover;
}

.home-feature-large {
  min-height: 402px;
  border-radius: 8px;
}

.home-feature-stack {
  min-width: 0;
  display: grid;
  grid-template-rows: repeat(2, minmax(0, 1fr));
  gap: 9px;
}

.home-feature-small {
  min-height: 0;
  border-radius: 8px;
}

.home-feature-overlay,
.home-library-overlay {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 34px 14px 13px;
  background: linear-gradient(180deg, transparent, rgba(17, 20, 22, .84));
  color: var(--proto-paper);
  opacity: 0;
  transition: opacity .18s ease;
}

.home-feature:hover .home-feature-overlay,
.home-feature:focus-visible .home-feature-overlay,
.home-library-item:hover .home-library-overlay,
.home-library-item:focus-visible .home-library-overlay {
  opacity: 1;
}

.home-feature-overlay strong,
.home-library-overlay strong {
  overflow: hidden;
  font-size: 14px;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.home-feature-overlay span,
.home-library-overlay span {
  color: rgba(241, 242, 237, .76);
  font-size: 11px;
}

.home-gallery-skeleton {
  grid-column: 1 / -1;
  min-height: 402px;
  padding: 40px;
  background: rgba(255, 255, 255, .5);
}

.home-gallery-empty {
  grid-column: 1 / -1;
  align-self: center;
  min-height: 300px;
  padding-top: 100px;
  background: rgba(255, 255, 255, .5);
}

.home-library,
.home-continue,
.home-admin {
  margin-top: 34px;
}

.home-section-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 13px;
}

.home-section-kicker {
  display: block;
  margin-bottom: 4px;
  color: var(--proto-orange);
  font-size: 11px;
  font-weight: 700;
}

.home-section-heading h2 {
  margin: 0;
  color: var(--proto-ink);
  font-size: 28px;
  line-height: 1.05;
  letter-spacing: -.055em;
}

.home-section-link {
  padding-bottom: 3px;
  color: var(--proto-ink);
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;
}

.home-section-link:hover {
  color: var(--proto-orange);
}

.home-alert {
  margin-bottom: 13px;
}

.home-library-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 9px;
}

.home-library-item {
  aspect-ratio: 1 / 1.16;
  border-radius: 8px;
  background: var(--proto-paper-deep);
}

.home-library-overlay {
  padding: 30px 11px 11px;
}

.home-library-overlay strong {
  font-size: 12px;
}

.home-library-overlay span {
  font-size: 10px;
}

.home-continue-heading {
  margin-bottom: 12px;
}

.home-continue-grid {
  display: grid;
  grid-template-columns: 1.16fr 1fr 1fr;
  gap: 9px;
}

.home-continue-item {
  position: relative;
  min-height: 118px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 17px 18px;
  color: var(--proto-ink);
  text-decoration: none;
  transition: box-shadow .18s ease, transform .18s ease;
}

.home-continue-item:hover {
  color: var(--proto-ink);
  transform: translateY(-2px);
  box-shadow: 0 12px 25px rgba(18, 23, 23, .12);
}

.home-continue-dark {
  background: var(--proto-ink);
  color: var(--proto-paper);
}

.home-continue-dark:hover {
  color: var(--proto-paper);
}

.home-continue-acid {
  background: var(--proto-acid);
}

.home-continue-paper {
  border: 1px solid var(--proto-line);
  background: rgba(255, 255, 255, .56);
}

.home-continue-label {
  font-size: 11px;
  opacity: .66;
}

.home-continue-item strong {
  margin-top: auto;
  font-size: 20px;
  letter-spacing: -.045em;
}

.home-continue-arrow {
  position: absolute;
  top: 15px;
  right: 17px;
  font-size: 19px;
}

.home-admin {
  padding-top: 26px;
  border-top: 1px solid var(--proto-line);
}

.home-admin-list {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border-top: 1px solid var(--proto-line);
  border-bottom: 1px solid var(--proto-line);
}

.home-admin-link {
  position: relative;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px 17px;
  color: var(--proto-ink);
  text-decoration: none;
}

.home-admin-link + .home-admin-link {
  border-left: 1px solid var(--proto-line);
}

.home-admin-link:hover {
  background: rgba(186, 255, 61, .22);
}

.home-admin-link span {
  font-size: 14px;
  font-weight: 700;
}

.home-admin-link small {
  color: var(--proto-muted);
  font-size: 11px;
}

.home-admin-link strong {
  position: absolute;
  top: 14px;
  right: 15px;
  color: var(--proto-orange);
  font-size: 16px;
}

@media (max-width: 980px) {
  .prototype-home {
    padding-top: 18px;
  }

  .home-hero {
    grid-template-columns: 1fr;
  }

  .home-hero-copy {
    min-height: 330px;
  }

  .home-hero-gallery {
    min-height: 360px;
  }
}

@media (max-width: 720px) {
  .home-library-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .home-continue-grid,
  .home-admin-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .home-admin-link:nth-child(3) {
    border-left: 0;
    border-top: 1px solid var(--proto-line);
  }

  .home-admin-link:nth-child(4) {
    border-top: 1px solid var(--proto-line);
  }
}

@media (max-width: 520px) {
  .prototype-home {
    padding-top: 12px;
  }

  .home-hero-copy {
    min-height: 330px;
    padding: 25px 22px 20px;
  }

  .home-hero-gallery {
    grid-template-columns: 1fr;
    min-height: 500px;
  }

  .home-feature-large {
    min-height: 300px;
  }

  .home-feature-stack {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    grid-template-rows: 150px;
  }

  .home-library-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .home-continue-grid,
  .home-admin-list {
    grid-template-columns: 1fr;
  }

  .home-admin-link + .home-admin-link,
  .home-admin-link:nth-child(3),
  .home-admin-link:nth-child(4) {
    border-top: 1px solid var(--proto-line);
    border-left: 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .home-continue-item,
  .home-feature-overlay,
  .home-library-overlay {
    transition: none;
  }
}
</style>
