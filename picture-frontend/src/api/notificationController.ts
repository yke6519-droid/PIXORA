/* eslint-disable */
import request from '../plugin/request'

/** 查询当前登录用户的通知分页。 */
export async function queryNotificationPage(
  body: API.NotificationQueryRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseIPageNotificationVO>('/notification/queryPage', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 当前用户批量删除通知。 */
export async function deleteNotifications(
  body: API.NotificationDeleteRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseInteger>('/notification/deleteByIds', {
    method: 'DELETE',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 当前用户批量标记通知已读。 */
export async function markNotificationsRead(
  body: API.NotificationReadRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseInteger>('/notification/readByIds', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
