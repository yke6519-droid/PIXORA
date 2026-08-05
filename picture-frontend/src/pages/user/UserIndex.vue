<template>
  <div id="userIndexPage">
    <!-- 未登录状态 -->
    <div v-if="!loginUserStore.loginUser" class="not-login">
      <a-result status="warning" title="您还未登录" sub-title="登录后可查看您的个人中心和上传的图片">
        <template #extra>
          <a-button type="primary" @click="router.push('/user/login')"> 立即登录 </a-button>
          <a-button @click="router.push('/user/register')"> 注册账号 </a-button>
        </template>
      </a-result>
    </div>

    <!-- 已登录状态 -->
    <div v-else class="user-content">
      <!-- 用户信息卡片 -->
      <a-card class="user-info-card" :bordered="false">
        <div class="user-info-content">
          <!-- 头像区域 -->
          <div class="avatar-section">
            <a-avatar :size="120" :src="loginUserStore.loginUser.avatarurl" class="user-avatar">
              <template #icon v-if="!loginUserStore.loginUser.avatarurl">
                {{ loginUserStore.loginUser.username?.charAt(0).toUpperCase() || 'U' }}
              </template>
            </a-avatar>
            <div class="user-role-badge">
              <a-tag :color="getRoleColor(loginUserStore.loginUser.userLevel)">
                {{ getRoleText(loginUserStore.loginUser.userLevel) }}
              </a-tag>
            </div>
          </div>

          <!-- 用户信息区域 -->
          <div class="info-section">
            <div class="user-name-row">
              <h1 class="user-name">{{ loginUserStore.loginUser.username }}</h1>
              <a-button type="primary" @click="showEditModal = true" class="edit-btn">
                <EditOutlined />
                编辑资料
              </a-button>
            </div>

            <div class="user-account">
              <span class="label">账号：</span>
              <span class="value">{{ loginUserStore.loginUser.useraccount }}</span>
            </div>

            <!-- 基本信息标签 -->
            <div class="info-tags">
              <a-tag v-if="loginUserStore.loginUser.gender === 0" color="blue"> <ManOutlined /> 男 </a-tag>
              <a-tag v-else-if="loginUserStore.loginUser.gender === 1" color="pink"> <WomanOutlined /> 女 </a-tag>
              <a-tag v-if="loginUserStore.loginUser.phone" color="cyan"> <PhoneOutlined /> {{ loginUserStore.loginUser.phone }} </a-tag>
              <a-tag v-if="loginUserStore.loginUser.email" color="orange"> <MailOutlined /> {{ loginUserStore.loginUser.email }} </a-tag>
            </div>

            <!-- 个人简介 -->
            <div class="user-profile" v-if="loginUserStore.loginUser.profile">
              <p class="profile-label">个人简介</p>
              <p class="profile-text">{{ loginUserStore.loginUser.profile }}</p>
            </div>

            <!-- 时间信息 -->
            <div class="time-info">
              <span> <CalendarOutlined /> 注册于 {{ formatTime(loginUserStore.loginUser.createtime) }} </span>
            </div>
          </div>
        </div>
      </a-card>

      <!-- 用户图片展示 -->
      <a-card class="user-pictures-card" :bordered="false">
        <template #title>
          <div class="card-title">
            <PictureOutlined />
            <span>我的图片</span>
            <span class="picture-count">({{ total }}张)</span>
          </div>
        </template>
        <template #extra>
          <a-button type="link" @click="router.push('/picture/manage')"> 管理我的图片 <ArrowRightOutlined /> </a-button>
        </template>

        <!-- 图片列表 -->
        <div v-if="userPictures.length > 0" class="picture-grid">
          <div v-for="picture in userPictures" :key="picture.id" class="picture-item" @click="goToPictureDetail(picture.id)">
            <div class="picture-image-wrapper">
              <img :src="picture.url" :alt="picture.name" />
              <div class="picture-overlay">
                <EyeOutlined />
              </div>
            </div>
            <div class="picture-info">
              <p class="picture-name" :title="picture.name">{{ picture.name }}</p>
              <p class="picture-meta">
                <span>{{ picture.picformat?.toUpperCase() }}</span>
                <span>{{ formatSize(picture.picsize) }}</span>
              </p>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <a-empty v-else-if="!loading" description="还没有上传过图片">
          <template #extra>
            <a-button type="primary" @click="uploadModalOpen = true"> <UploadOutlined /> 上传图片 </a-button>
          </template>
        </a-empty>

        <!-- 加载状态 -->
        <div v-else class="loading-state">
          <a-spin size="large" />
        </div>
      </a-card>

      <!-- 快捷操作 -->
      <a-card class="quick-actions-card" :bordered="false">
        <template #title>
          <div class="card-title">
            <ThunderboltOutlined />
            <span>快捷操作</span>
          </div>
        </template>

        <div class="action-grid">
          <div class="action-item" @click="uploadModalOpen = true">
            <div class="action-icon upload">
              <UploadOutlined />
            </div>
            <span class="action-text">上传图片</span>
          </div>
          <div class="action-item" @click="router.push('/picture/manage')">
            <div class="action-icon manage">
              <AppstoreOutlined />
            </div>
            <span class="action-text">图片管理</span>
          </div>
          <div class="action-item" @click="router.push('/picture')">
            <div class="action-icon browse">
              <PictureOutlined />
            </div>
            <span class="action-text">浏览图片</span>
          </div>
          <div class="action-item" @click="showEditModal = true">
            <div class="action-icon setting">
              <SettingOutlined />
            </div>
            <span class="action-text">账号设置</span>
          </div>
        </div>
      </a-card>
    </div>

    <!-- 图片上传弹窗组件 -->
    <PictureUploadModal
      v-model:open="uploadModalOpen"
      :category-list="categoryList"
      :tag-list="tagList"
      @success="handleUploadSuccess"
    />

    <!-- 用户编辑弹窗组件 -->
    <UserEditModal
      v-model:open="showEditModal"
      title="编辑个人资料"
      :user-data="loginUserStore.loginUser"
      :is-admin="false"
      @save="handleSave"
      @cancel="handleCancel"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '../../stores/useLoginUserStore'
import {
  EditOutlined,
  ManOutlined,
  WomanOutlined,
  PhoneOutlined,
  MailOutlined,
  CalendarOutlined,
  PictureOutlined,
  ArrowRightOutlined,
  EyeOutlined,
  UploadOutlined,
  ThunderboltOutlined,
  AppstoreOutlined,
  SettingOutlined,
  PlusOutlined,
  CameraOutlined
  } from '@ant-design/icons-vue'
import type { UploadProps } from 'ant-design-vue'
import { queryPicturePageUsingPost,queryPicturePageCacheUsingPost, listPictureCategoryUsingGet } from '../../api/pictureController'
import { updateSelfUsingPost } from '../../api/userController'
import PictureUploadModal from '../../components/PictureUploadModal.vue'
import UserEditModal from '../../components/UserEditModal.vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 用户图片列表
const userPictures = ref<API.PictureVO[]>([])
const total = ref(0)
const loading = ref(false)

// 编辑弹窗
const showEditModal = ref(false)

// 上传弹窗相关
const uploadModalOpen = ref(false)
const categoryList = ref<string[]>([])
const tagList = ref<string[]>([])



/**
 * 获取用户上传的图片（最新5张）
 */
const fetchUserPictures = async () => {
  if (!loginUserStore.loginUser?.id) return

  loading.value = true
  
  try {
      const res = await queryPicturePageCacheUsingPost({
        userId: loginUserStore.loginUser.id,
        current: 1,
        pageSize: 5,
      })
      if (res.data.code === 200) {
        userPictures.value = res.data.data?.pictureList || []
        total.value = res.data.data?.total || 0
      } else {
        console.error('获取图片失败:', res.data.message)
      }
  } catch (error) {
    console.error('获取图片失败:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 保存用户信息
 */
const handleSave = async (formData: API.UpdateSelfRequest) => {
  try {
    const res = await updateSelfUsingPost(formData)
    if (res.data.code === 200) {
      message.success('资料更新成功！')
      // 刷新用户信息
      await loginUserStore.fetchLoginUser()
      showEditModal.value = false
    } else {
      message.error(res.data.message || '更新失败')
    }
  } catch (error) {
    message.error('更新失败，请重试')
  }
}

/**
 * 取消编辑
 */
const handleCancel = () => {
  showEditModal.value = false
}

/**
 * 获取角色颜色
 */
const getRoleColor = (status?: string) => {
  const colorMap: Record<string, string> = {
    admin: 'red',
    vip: 'gold',
    user: 'blue',
  }
  return colorMap[status || 'user'] || 'blue'
}

/**
 * 获取角色文本
 */
const getRoleText = (status?: string) => {
  const textMap: Record<string, string> = {
    admin: '管理员',
    vip: 'VIP会员',
    user: '普通用户',
  }
  return textMap[status || 'user'] || '普通用户'
}

/**
 * 格式化时间
 */
const formatTime = (time?: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleDateString('zh-CN')
}

/**
 * 格式化文件大小
 */
const formatSize = (size?: number) => {
  if (!size) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / (1024 * 1024)).toFixed(1) + ' MB'
}

/**
 * 跳转到图片详情
 */
const goToPictureDetail = (id?: number) => {
  if (id) {
    router.push(`/picture/detail/${id}`)
  }
}

/**
 * 获取分类和标签
 */
const fetchCategoryAndTags = async () => {
  try {
    const res = await listPictureCategoryUsingGet()
    if (res.data.code === 200) {
      categoryList.value = res.data.data?.categorys || []
      tagList.value = res.data.data?.tags || []
    }
  } catch (error) {
    console.error('获取分类标签失败', error)
  }
}

/**
 * 上传成功回调
 */
const handleUploadSuccess = () => {
  // 刷新图片列表
  fetchUserPictures()
}



onMounted(async () => {
  await loginUserStore.fetchLoginUser()
  if (loginUserStore.loginUser) {
    fetchUserPictures()
    fetchCategoryAndTags()
  }
})
</script>

<style scoped>
#userIndexPage {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

/* 未登录状态 */
.not-login {
  padding: 60px 0;
}

/* 用户内容区 */
.user-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 用户信息卡片 */
.user-info-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  overflow: hidden;
}

.user-info-card :deep(.ant-card-body) {
  padding: 32px;
}

.user-info-content {
  display: flex;
  gap: 40px;
  color: #fff;
}

/* 头像区域 */
.avatar-section {
  position: relative;
  flex-shrink: 0;
}

.user-avatar {
  border: 4px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}

.user-role-badge {
  position: absolute;
  bottom: -8px;
  left: 50%;
  transform: translateX(-50%);
}

/* 用户信息区域 */
.info-section {
  flex: 1;
  min-width: 0;
}

.user-name-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.user-name {
  margin: 0;
  font-size: 32px;
  font-weight: 600;
  color: #fff;
}

.edit-btn {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: #fff;
}

.edit-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  border-color: rgba(255, 255, 255, 0.5);
}

.user-account {
  margin-bottom: 16px;
  font-size: 14px;
  opacity: 0.9;
}

.user-account .label {
  opacity: 0.7;
}

.info-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
}

.info-tags :deep(.ant-tag) {
  margin: 0;
}

.user-profile {
  margin-bottom: 16px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
}

.profile-label {
  margin: 0 0 8px 0;
  font-size: 12px;
  opacity: 0.7;
}

.profile-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
}

.time-info {
  font-size: 12px;
  opacity: 0.7;
}

/* 图片卡片 */
.user-pictures-card {
  border-radius: 16px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
}

.picture-count {
  font-weight: normal;
  color: #999;
  font-size: 14px;
}

.picture-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

.picture-item {
  cursor: pointer;
  transition: transform 0.3s;
}

.picture-item:hover {
  transform: translateY(-4px);
}

.picture-image-wrapper {
  position: relative;
  width: 100%;
  height: 140px;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
}

.picture-image-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.picture-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
  color: #fff;
  font-size: 24px;
}

.picture-item:hover .picture-overlay {
  opacity: 1;
}

.picture-info {
  padding: 8px 4px;
}

.picture-name {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.picture-meta {
  margin: 0;
  font-size: 12px;
  color: #999;
}

.picture-meta span {
  margin-right: 8px;
}

.loading-state {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

/* 快捷操作卡片 */
.quick-actions-card {
  border-radius: 16px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 24px 16px;
  background: #fafafa;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.action-item:hover {
  background: #f0f0f0;
  transform: translateY(-2px);
}

.action-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  font-size: 24px;
  color: #fff;
}

.action-icon.upload {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.action-icon.manage {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.action-icon.browse {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.action-icon.setting {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.action-text {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

/* 编辑表单 */
.edit-form {
  padding: 16px 0;
}

.avatar-upload-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.avatar-upload-section :deep(.ant-upload) {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  padding: 0;
  border: 2px dashed #d9d9d9;
}

.avatar-upload-section :deep(.ant-upload:hover) {
  border-color: #1890ff;
}

.avatar-preview {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
}

.avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-mask {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.3s;
}

.avatar-preview:hover .avatar-mask {
  opacity: 1;
}

.avatar-mask .anticon {
  font-size: 24px;
  margin-bottom: 4px;
}

.upload-placeholder {
  width: 120px;
  height: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
}

.upload-placeholder .anticon {
  font-size: 32px;
  margin-bottom: 8px;
}

.upload-text {
  font-size: 12px;
}

.avatar-url-section {
  display: flex;
  align-items: center;
  gap: 8px;
}

.url-label {
  color: #999;
  font-size: 13px;
  white-space: nowrap;
}

/* 响应式适配 */
@media (max-width: 768px) {
  #userIndexPage {
    padding: 16px;
  }

  .user-info-content {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .user-name-row {
    flex-direction: column;
  }

  .info-tags {
    justify-content: center;
  }

  .picture-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .action-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .avatar-upload {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
