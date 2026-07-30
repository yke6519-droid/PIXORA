// @ts-ignore
/* eslint-disable */
import request from "../plugin/request";

/** addUser PUT /user/addUser */
export async function addUserUsingPut(
  body: API.AddUserRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/user/addUser", {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** deleteUser DELETE /user/deleteById */
export async function deleteUserUsingDelete(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/user/deleteById", {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** deleteUsers DELETE /user/deleteByIds */
export async function deleteUsersUsingDelete(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/user/deleteByIds", {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** getCurrentUser GET /user/getCurrentUser */
export async function getCurrentUserUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseUser_>("/user/getCurrentUser", {
    method: "GET",
    ...(options || {}),
  });
}

/** queryPages POST /user/queryPages */
export async function queryPagesUsingPost(
  body: API.QueryPageRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseUserPagesVO_>("/user/queryPages", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** getUserById GET /user/queryUserById */
export async function getUserByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getUserByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseUser_>("/user/queryUserById", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** getAllUsers GET /user/queryUsers */
export async function getAllUsersUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseListUser_>("/user/queryUsers", {
    method: "GET",
    ...(options || {}),
  });
}

/** userRegister POST /user/register */
export async function userRegisterUsingPost(
  body: API.RegisterRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/user/register", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** updateUser POST /user/update */
export async function updateUserUsingPost(
  body: API.UpdateUserRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/user/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** updateSelf POST /user/updateSelf */
export async function updateSelfUsingPost(
  body: API.UpdateSelfRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/user/updateSelf", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** userLogin POST /user/userLogin */
export async function userLoginUsingPost(
  body: API.UserLoginRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseString_>("/user/userLogin", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** userLogout GET /user/userLogout */
export async function userLogoutUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseString_>("/user/userLogout", {
    method: "GET",
    ...(options || {}),
  });
}
