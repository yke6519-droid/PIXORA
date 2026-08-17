import assert from 'node:assert/strict'
import test from 'node:test'

import { decideAvatarReview } from '../src/pages/prototype/admin/avatarReviewModel.ts'

const pendingReview = {
  id: 'avatar-check-001',
  userId: '12',
  username: '林默',
  useraccount: 'linmo',
  avatarUrl: 'https://i.pravatar.cc/160?img=12',
  status: 0,
  submittedAt: '2026-08-16 09:24',
}

test('管理员通过头像后，记录进入审核通过状态', () => {
  assert.deepEqual(decideAvatarReview(pendingReview, 1), {
    ...pendingReview,
    status: 1,
    checkMessage: '审核通过',
  })
})

test('管理员拒绝头像后，记录保存审核原因并进入失败状态', () => {
  assert.deepEqual(decideAvatarReview(pendingReview, 2, '头像内容不符合社区规范'), {
    ...pendingReview,
    status: 2,
    checkMessage: '头像内容不符合社区规范',
  })
})

test('拒绝头像时未填写原因会阻止状态变更', () => {
  assert.throws(
    () => decideAvatarReview(pendingReview, 2, '  '),
    /请填写审核拒绝原因/,
  )
})
