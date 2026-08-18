export type AvatarUploadOutcomeStatus = 'pending' | 'approved' | 'invalid'

export interface AvatarUploadPayload {
  status?: number
  newURL?: string
  message?: string
}

export interface AvatarUploadOutcome {
  status: AvatarUploadOutcomeStatus
  avatarUrl?: string
  message: string
}

/**
 * 将后端头像上传结果转换成页面可直接消费的状态，避免待审核头像误替换当前头像。
 */
export function resolveAvatarUploadOutcome(payload?: AvatarUploadPayload): AvatarUploadOutcome {
  const avatarUrl = payload?.newURL?.trim()

  if (payload?.status === 0) {
    return {
      status: 'pending',
      avatarUrl: undefined,
      message: '头像已提交，等待管理员审核',
    }
  }

  if (payload?.status === 1 && avatarUrl) {
    return {
      status: 'approved',
      avatarUrl,
      message: '头像已更新',
    }
  }

  return {
    status: 'invalid',
    avatarUrl: undefined,
    message: payload?.message?.trim() || '头像上传结果异常',
  }
}
