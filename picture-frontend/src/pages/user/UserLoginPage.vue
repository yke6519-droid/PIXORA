<template>
    <div id="userLoginPage">
        <h1 class="title">智能云图库 - 用户登录</h1>
        <div class="desc">企业级智能协同云图库</div>
        <a-form
            :model="formState"
            name="basic"
            :label-col="{ span: 8 }"
            :wrapper-col="{ span: 16 }"
            autocomplete="off"
            @finish="onFinish"
        >
            <a-form-item :wrapper-col="{ offset: 4, span: 16 }"
            name="useraccount"
            :rules="[{ required: true, message: '账号不得为空!' },
                {min: 6, message: '账号长度不得小于6位'},
                {max: 20, message: '账号长度不得大于20位'}
            ]"
            >
            <a-input v-model:value="formState.useraccount" placeholder="请输入账号"/>
            </a-form-item>

            <a-form-item :wrapper-col="{ offset: 4, span: 16 }"
            name="userpassword"
            :rules="[{ required: true, message: '密码不得为空' },
                {min: 6, message: '密码长度不得小于6位'},
                {max: 20, message: '密码长度不得大于20位'}
            ]"
            >
            <a-input-password v-model:value="formState.userpassword"placeholder="请输入密码" />
            </a-form-item>

            <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
            
            <div style="text-align: right; font-size: 13px; color: #bbb; margin-bottom: 16px;">
                没有账号？<RouterLink to="/user/register">去注册</RouterLink>
            </div>
            <a-button type="primary" html-type="submit"  style="width: 100%;">登录</a-button> 
            </a-form-item>

        </a-form>
    </div>
</template>
<script lang="ts" setup>
import { ref } from 'vue';
import { useLoginUserStore } from '../../stores/useLoginUserStore';
import { useRouter } from 'vue-router';
import { userLoginUsingPost } from '../../api/userController';
import { message } from 'ant-design-vue';

const router = useRouter()
const loginUserStore = useLoginUserStore()
const formState = ref<API.UserLoginRequest>({
    useraccount: '',
    userpassword: '',
});

/**
 * 提交登录
 * @param values 
 */
const onFinish = async(values: any) => {
    const res = await userLoginUsingPost({
        useraccount: values.useraccount,
        userpassword: values.userpassword
    })
    if(res.data.code===200){
        message.success('登录成功！')
        await loginUserStore.fetchLoginUser()
        router.push(
            {path: '/',
            replace: true}
        )
    }else{
        message.error(`登录失败！${res.data.message}`)
    }
};

</script>

<style scoped>
#userLoginPage{
    max-width: 729px;
    margin: 0 auto;
}
.title{
    margin-top: 50px;
    text-align: center;
    line-height: 50px;
}
.desc{
    margin-top: 16px;
    margin-bottom: 16px;
    color: #bbb;
    line-height: 50px;
}
</style>
