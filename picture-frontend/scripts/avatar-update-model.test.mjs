import assert from 'node:assert/strict'
import test from 'node:test'

import { resolveAvatarUploadOutcome } from '../src/pages/prototype/user/avatarUpdateModel.ts'

test('普通用户上传头像只进入待审核状态，不返回可直接替换的地址', () => {
  assert.deepEqual(resolveAvatarUploadOutcome({ status: 0 }), {
    status: 'pending',
    avatarUrl: undefined,
    message: '头像已提交，等待管理员审核',
  })
})

test('管理员上传头像返回已通过状态和新地址', () => {
  assert.deepEqual(resolveAvatarUploadOutcome({ status: 1, newURL: '  https://cdn.example.com/avatar.png  ' }), {
    status: 'approved',
    avatarUrl: 'https://cdn.example.com/avatar.png',
    message: '头像已更新',
  })
})

test('缺少可用地址的上传结果不能被当作成功', () => {
  assert.deepEqual(resolveAvatarUploadOutcome({ status: 1 }), {
    status: 'invalid',
    avatarUrl: undefined,
    message: '头像上传结果异常',
  })
})
