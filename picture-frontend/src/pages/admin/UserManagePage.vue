<template>
    <div id="userInfoShower">
        <!-- 管理员可见 -->
        <template  v-if="LoginUserStore.loginUser?.userLevel === 'admin'">
            <!-- 搜索表单 -->
            <a-form
                class="searchForm"
                layout="inline"
                :model="searchParams"
                @finish="queryPages"
            >
                <a-form-item>
                    <a-input v-model:value="searchParams.username" placeholder="请输入你要查询的用户名">
                        <template #prefix><UserOutlined style="color: rgba(0, 0, 0, 0.25)" /></template>
                    </a-input>
                </a-form-item>
                
                <a-form-item>
                    <a-input v-model:value="searchParams.useraccount" placeholder="请输入你要查询的账号">
                        <template #prefix><LockOutlined style="color: rgba(0, 0, 0, 0.25)" /></template>
                    </a-input>
                </a-form-item>
                
                <a-form-item label="性别">
                    <a-select
                        v-model:value="searchParams.gender"
                        placeholder="选择性别"
                        allow-clear
                        style="width: 120px"
                    >
                        <a-select-option :value="0">
                            男
                        </a-select-option>
                        <a-select-option :value="1">
                            女
                        </a-select-option>
                    </a-select>
                </a-form-item>
                
                <a-form-item label="角色">
                    <a-select
                        v-model:value="searchParams.userLevel"
                        placeholder="选择角色"
                        allow-clear
                        style="width: 140px"
                    >
                        <a-select-option value="admin">
                            <a-tag color="green">管理员</a-tag>
                        </a-select-option>
                        <!-- <a-select-option value="vip">
                            <a-tag color="gold">VIP会员</a-tag>
                        </a-select-option> -->
                        <a-select-option value="user">
                            <a-tag color="blue">普通用户</a-tag>
                        </a-select-option>
                    </a-select>
                </a-form-item>
                
                <a-form-item>
                    <a-button
                        type="primary"
                        html-type="submit"
                    >
                        <SearchOutlined /> 搜索
                    </a-button>
                </a-form-item>
                
                <a-form-item>
                    <a-button @click="resetSearch">
                        <ReloadOutlined /> 重置
                    </a-button>
                </a-form-item>
            </a-form>
            
             <!-- 结果表单 -->
            <a-table
            class="userInfoList"
            :columns="columns" 
            :data-source="UserList"
            :pagination="{
                current: currentPage,
                total: totalSize,
                pageSize: pageSize,
                showSizeChanger: false,
                showTotal: (total:any) => `共 ${total} 条`
            }"
            @change="handleTableChange"
            >
                <!-- 表头 -->
                <template #headerCell="{ column }">
                    <template v-if="column.key === 'name'">
                        <span>
                        <smile-outlined />
                        用户昵称
                        </span>
                    </template>
                </template>
                <!-- 信息列 -->
                <template #bodyCell="{ column, record }">
                    <template v-if="column.key === 'avatarurl'">
                        <a>
                            <a-avatar :src="record.avatarurl" />
                        </a>
                    </template>
                    <template v-if="column.key === 'createtime'">
                        <div>
                        {{ dayjs(record.createtime).format('YYYY-MM-DD HH:mm:ss') }}
                        </div>
                    </template>
                    <template v-if="column.key === 'userLevel'">
                        <a-tag v-if="record.userLevel==='admin'" color="green">管理员</a-tag>
                        <a-tag v-if="record.userLevel==='user'" color="blue">普通用户</a-tag>
                        <a-tag v-if="record.userLevel==='vip'" color="gold">VIP会员</a-tag>
                    </template>
                    <template v-if="column.key === 'gender'">
                        <div v-if="record.gender===0">
                             男
                        </div>
                        <div v-else-if="record.gender===1">
                            女
                        </div>
                        <div v-else>
                            未知
                        </div>
                    </template>
                    <template v-if="column.key === 'email'">
                        <div v-if="!record.email">
                            暂无数据
                        </div>
                    </template>
                    <template v-else-if="column.key === 'action'">
                        <span>
                        <a-button type="primary" size="small" @click="showModal(record)">
                            <EditOutlined /> 编辑
                        </a-button>
                        <a-divider type="vertical" />
                        <a-button 
                            type="primary" 
                            danger 
                            size="small" 
                            @click="doDelete(record.id)" 
                            :disabled="LoginUserStore.loginUser.id === record.id || record.userLevel==='admin'"
                        >
                            <DeleteOutlined /> 删除
                        </a-button>
                        </span>
                    </template>
                </template>
            </a-table>
        </template>
        <!-- 未登录 -->
        <template v-else-if="LoginUserStore.loginUser===null">
            <a-result
                status="warning"
                title="您还未登录"
                sub-title="登录后可访问用户管理页面"
            >
                <template #extra>
                    <a-button type="primary" @click="router.push('/user/login')">
                        立即登录
                    </a-button>
                </template>
            </a-result>
        </template>
        <!-- 非管理员无权限 -->
        <template v-else>
            <a-result
                status="403"
                title="无权限访问"
                sub-title="抱歉，您没有权限访问用户管理页面"
            >
                <template #extra>
                    <a-button type="primary" @click="router.push('/')">
                        返回首页
                    </a-button>
                </template>
            </a-result>
        </template>

        <!-- 用户编辑弹窗组件 -->
        <UserEditModal
            v-model:open="editModalOpen"
            title="修改用户信息"
            :user-data="currentEditUser"
            :is-admin="true"
            @save="handleSave"
            @cancel="handleCancel"
        />
    </div>
</template>

<script lang="ts" setup>

import { onMounted, ref} from 'vue';
import { deleteUserUsingDelete, queryPagesUsingPost, updateUserUsingPost } from '../../api/userController';
import { message } from 'ant-design-vue';
import { useLoginUserStore } from '../../stores/useLoginUserStore';
import { useRouter } from 'vue-router';
import dayjs from 'dayjs';
import UserEditModal from '../../components/UserEditModal.vue';
import {
    UserOutlined,
    LockOutlined,
    SearchOutlined,
    ReloadOutlined,
    EditOutlined,
    DeleteOutlined,
    SmileOutlined
} from '@ant-design/icons-vue';

const router = useRouter();

// 编辑弹窗相关
const editModalOpen = ref(false)
const currentEditUser = ref<API.UserVO | undefined>(undefined)

/**
 * 控制编辑信息栏弹窗
 */
const showModal = (userInfo: API.UserVO) => {
  editModalOpen.value = true
  currentEditUser.value = userInfo
}

/**
 * 保存用户信息
 */
const handleSave = async (formData: API.UpdateUserRequest) => {
  const res = await updateUserUsingPost({
    id: formData.id,
    avatarurl: formData.avatarurl,
    email: formData.email,
    gender: formData.gender,
    phone: formData.phone,
    profile: formData.profile,
    username: formData.username,
    userLevel: currentEditUser.value?.userLevel
  })

  if (res.data.code === 200) {
    message.success('更新成功！')
    editModalOpen.value = false
    await queryPages()
  } else {
    message.error(`更新失败！${res.data.message}`)
  }
}

/**
 * 取消编辑
 */
const handleCancel = () => {
  editModalOpen.value = false
  currentEditUser.value = undefined
}

/**
 * 表头展示列
 */
const columns = [
  {
    title: '用户昵称',
    dataIndex: 'username',
    key: 'username',
    width: 120,
  },
  {
    title: '账号',
    dataIndex: 'useraccount',
    key: 'useraccount',
    width: 120,
  },
  {
    title: '头像',
    dataIndex: 'avatarurl',
    key: 'avatarurl',
    width: 80,
  },
  {
    title: '用户角色',
    dataIndex: 'userLevel',
    key: 'userLevel',
    width: 100,
  },
  {
    title: '性别',
    dataIndex: 'gender',
    key: 'gender',
    width: 80,
  },
  {
    title: '联系方式',
    key: 'phone',
    dataIndex: 'phone',
    width: 120,
  },
  {
    title: '电子邮件',
    key: 'email',
    dataIndex: 'email',
    width: 180,
    ellipsis: true,
  },
  {
    title: '创建时间',
    key: 'createtime',
    dataIndex: 'createtime',
    width: 180,
  },
  {
    title: '操作',
    key: 'action',
    width: 180,
    fixed: 'right',
  },
];

const LoginUserStore = useLoginUserStore()

/**
 * 删除对应id的用户
 */
const doDelete=async(id:any)=>{
    const res = await deleteUserUsingDelete({
        id: id
    })
    console.log(res.data)
    if(res.data.code===200){
        message.success("已成功删除该用户！")
        await queryPages()
    }else{
        message.error("删除用户时出现异常：" + res.data.message)
    }
}

/**
 * 用户列表数据
 */
const UserList:any = ref([]);

const totalSize:any = ref(0)
const currentPage = ref(1)
const pageSize = 7

/**
 * 换页触发的函数
 */
const handleTableChange = (pagination:any) => {
  currentPage.value = pagination.current;
  queryPages();
};

/**
 * 查询条件参数
 */
const searchParams = ref({
    username: '',
    useraccount: '',
    gender: undefined as number | undefined,
    userLevel: undefined as string | undefined
})

/**
 * 重置搜索
 */
const resetSearch = () => {
    searchParams.value = {
        username: '',
        useraccount: '',
        gender: undefined,
        userLevel: undefined
    }
    currentPage.value = 1;
    queryPages();
}

/**
 * 分页查询
 */
const queryPages=async()=>{
    const res = await queryPagesUsingPost({
        current: currentPage.value,
        size: pageSize,
        queryUserAccount: searchParams.value.useraccount || undefined,
        queryUsername: searchParams.value.username || undefined,
        userLevel: searchParams.value.userLevel || undefined,
        // @ts-ignore
        gender: searchParams.value.gender
    })
    console.log(res.data)
    UserList.value = res.data.data?.userList || []
    console.log(UserList.value)
    totalSize.value = res.data.data?.totalSize || 0
    console.log(totalSize.value)
}

onMounted(async()=>{
    await queryPages()
})
</script>


<style scoped>
#userInfoShower {
    margin-top: 10px;
    text-align: center;
    align-items: center;
    line-height: 50px;
    padding: 0 24px;
}

h1 {
    margin-bottom: 24px;
    color: #333;
    font-weight: 600;
}

.userInfoList {
    max-width: 1400px;
    margin: 24px auto;
    background: #fff;
    border-radius: 8px;
    padding: 16px;
}

.searchForm{
    max-width: 1400px;
    margin: 0 auto 16px;
    padding: 20px;
    background: #fff;
    border-radius: 8px;
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
}

.statistics {
    max-width: 1400px;
    margin: 0 auto 16px;
    padding: 12px 20px;
    background: #f6f8fb;
    border-radius: 8px;
    text-align: left;
}

.stat-item {
    margin-right: 24px;
    color: #666;
}

.stat-item strong {
    color: #1890ff;
    font-size: 16px;
}

.avatar-uploader > .ant-upload {
  width: 128px;
  height: 128px;
}

.ant-upload-select-picture-card i {
  font-size: 32px;
  color: #999;
}

.ant-upload-select-picture-card .ant-upload-text {
  margin-top: 8px;
  color: #666;
}

.avatar-img {
  width: 128px;
  height: 128px;
  object-fit: cover;
  display: block;
}

.avatar-edit {
    display: flex;
    align-items: center;
}

.avatar-url-input {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 8px;
}

.url-label {
    color: #999;
    font-size: 13px;
    white-space: nowrap;
}
</style>
