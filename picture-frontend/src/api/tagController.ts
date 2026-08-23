import request from "../plugin/request";

/** 查询个人空间中可用于筛选和绑定的标签。 */
export async function listTag(
  params: API.listTagParams,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseListTag>("/tag/list", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 查询个人空间的全部标签，供标签管理页面使用。 */
export async function listManageTag(
  params: API.listTagParams,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseListTag>("/tag/manage/list", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 在个人空间创建标签。 */
export async function createTag(
  body: API.TagCreateRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseTag>("/tag/create", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 修改标签名称，图片绑定关系不变。 */
export async function renameTag(
  body: API.TagRenameRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean>("/tag/rename", {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 删除标签实体，并解除其全部图片关联。 */
export async function deleteTag(
  body: API.TagIdRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean>("/tag/delete", {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 管理员停用标签，历史关联仍然保留。 */
export async function disableTag(
  body: API.TagIdRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean>("/tag/disable", {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 管理员恢复标签。 */
export async function restoreTag(
  body: API.TagIdRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean>("/tag/restore", {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
