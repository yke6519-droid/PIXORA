import request from "../plugin/request";

/** 查询公共图库主题。 */
export async function listCategory(options?: { [key: string]: any }) {
  return request<API.BaseResponseListCategory>("/category/list", {
    method: "GET",
    ...(options || {}),
  });
}

/** 管理员新增公共图库主题。 */
export async function createCategory(
  body: API.CategoryCreateRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseCategory>("/category/create", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
