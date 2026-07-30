<template>
    <div id="userRegisterPage">
        <h1 class="title">智能云图库 - 用户注册</h1>
        <div class="desc">企业级智能协同云图库</div>
        <a-form
            :model="formState"
            name="basic"
            :label-col="{ span: 8 }"
            :wrapper-col="{ span: 16 }"
            autocomplete="off"
            @finish="onFinish"
            @finishFailed="onFinishFailed"
        >   

            <a-form-item :wrapper-col="{ offset: 4, span: 16 }"
            name="username"
            :rules="[{ required: true, message: '昵称不得为空' },
                {max: 20, message: '昵称长度不得大于20位'}
            ]"
            >
            <a-input v-model:value="formState.username" placeholder="请输入昵称"/>
            </a-form-item>

            <a-form-item :wrapper-col="{ offset: 4, span: 16 }"
            name="gender"
            :rules="[{ required: true, message: '性别不得为空' },]"
            >
                <a-radio-group v-model:value="formState.gender" button-style="solid">
                    <a-radio :value= "0">男</a-radio>
                    <a-radio :value= "1">女</a-radio>
                </a-radio-group>
            </a-form-item>

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
            <a-input-password v-model:value="formState.userpassword" placeholder="请输入密码"/>
            </a-form-item>
 
            <a-form-item :wrapper-col="{ offset: 4, span: 16 }"
            name="reUserPassword"
            :rules="[{ required: true, message: '密码不得为空' },
                {min: 6, message: '密码长度不得小于6位'},
                {max: 20, message: '密码长度不得大于20位'},
                {pattern: formState.reUserPassword===formState.userpassword, message: '两次输入的密码不同！'}
            ]"
            >
            <a-input-password v-model:value="formState.reUserPassword" placeholder="请确认密码"/>
            </a-form-item>

            <a-form-item :wrapper-col="{ offset: 4, span: 16 }"
            name="phone"
            :rules="[{ required: true, message: '手机号不得为空' },]"
            >
            <a-input v-model:value="formState.phone" placeholder="请输入手机号"/>
            </a-form-item>
        

            <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
                <a-button type="dashed" href="/user/login" style="margin-right: 16px;">返回</a-button> 
                <a-button type="primary" html-type="submit" style="width: 80%;">注册</a-button> 
            </a-form-item>


        </a-form>
    </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue';
import { useLoginUserStore } from '../../stores/useLoginUserStore';
import { useRouter } from 'vue-router';
import { userRegisterUsingPost } from '../../api/userController';
import { message } from 'ant-design-vue';

const router = useRouter()
const loginUserStore = useLoginUserStore()
const formState = ref<API.RegisterRequest>({
    gender: 0,
    phone: '',
    reUserPassword: '',
    useraccount: '',
    username: '',
    userpassword: '',
});
/**
 * 提交注册
 * @param values 
 */
const onFinish = async(values: any) => {
    const res = await userRegisterUsingPost({
        gender: values.gender,
        phone: values.phone,
        username: values.username,
        useraccount: values.useraccount,
        userpassword: values.userpassword,
        reUserPassword: values.reUserPassword,
    })
    if(res.data.code===200){
        message.success('注册成功')
        console.log(loginUserStore.loginUser)
        await loginUserStore.fetchLoginUser()
        console.log(loginUserStore.loginUser)
        router.push('/user/login')
    }
};

const onFinishFailed = (errorInfo: any) => {
  message.error(errorInfo)
};
</script>

<style scoped>
#userRegisterPage{
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
