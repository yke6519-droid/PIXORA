// @ts-ignore
/* eslint-disable */
import request from "../plugin/request";

/** 此处后端没有提供注释 PUT /user/addUser */
export async function addUser(
  body: API.AddUserRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/user/addUser", {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 DELETE /user/deleteById */
export async function deleteUser(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/user/deleteById", {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 DELETE /user/deleteByIds */
export async function deleteUsers(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/user/deleteByIds", {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /user/getCurrentUser */
export async function getCurrentUser(options?: { [key: string]: any }) {
  return request<API.BaseResponseUserVO>("/user/getCurrentUser", {
    method: "GET",
    ...(options || {}),
  });
}

/** 管理员查询头像审核列表。 */
export async function queryAvatarReviews(options?: { [key: string]: any }) {
  return request<API.BaseResponseListAvatarReviewVO>("/user/queryAvatarReviews", {
    method: "GET",
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /user/queryPages */
export async function queryPages(
  body: API.QueryPageRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseUserPagesVO>("/user/queryPages", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /user/queryUserById */
export async function getUserById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getUserByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseUserVO>("/user/queryUserById", {
    method: "GET",
    params: {
      ...params,
      queryUserRequest: undefined,
      ...params["queryUserRequest"],
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /user/queryUsers */
export async function getAllUsers(options?: { [key: string]: any }) {
  return request<API.BaseResponseListUserVO>("/user/queryUsers", {
    method: "GET",
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /user/register */
export async function userRegister(
  body: API.RegisterRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/user/register", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /user/update */
export async function updateUser(
  body: API.UpdateUserRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/user/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /user/updateSelf */
export async function updateSelf(
  body: API.UpdateSelfRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/user/updateSelf", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 管理员提交单条头像审核结果。 */
export async function adminCheckAvatar(
  body: API.AdminCheckAvatarRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/user/adminCheckAvatar", {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /user/userLogin */
export async function userLogin(
  body: API.UserLoginRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseString>("/user/userLogin", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /user/userLogout */
export async function userLogout(options?: { [key: string]: any }) {
  return request<API.BaseResponseString>("/user/userLogout", {
    method: "GET",
    ...(options || {}),
  });
}
