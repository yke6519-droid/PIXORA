export type AvatarReviewStatus = 0 | 1 | 2
export type AvatarReviewDecision = 1 | 2

export interface AvatarReviewItem {
  id: string
  userId: string
  username: string
  useraccount: string
  avatarUrl: string
  status: AvatarReviewStatus
  submittedAt: string
  checkMessage?: string
  reviewedAt?: string
}

/**
 * 前端审核结果输入校验的纯函数，真实状态变更由后端接口完成。
 * 拒绝原因是业务上可观察的必填条件，因此在模型层统一拦截空值。
 */
export function decideAvatarReview(
  item: AvatarReviewItem,
  decision: AvatarReviewDecision,
  checkMessage = '',
): AvatarReviewItem {
  const normalizedMessage = checkMessage.trim()
  if (decision === 2 && !normalizedMessage) {
    throw new Error('请填写审核拒绝原因')
  }

  return {
    ...item,
    status: decision,
    checkMessage: decision === 1 ? '审核通过' : normalizedMessage,
  }
}
