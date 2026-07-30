// @ts-ignore
/* eslint-disable */
import request from "../plugin/request";

/** alterLevelById PUT /space/alterLevelById */
export async function alterLevelByIdUsingPut(
  body: API.AlterLevelRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/space/alterLevelById", {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** createSpace POST /space/createSpace */
export async function createSpaceUsingPost(
  body: API.CreateSpaceRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseSpace_>("/space/createSpace", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** deleteById DELETE /space/deleteById */
export async function deleteByIdUsingDelete(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteByIdUsingDELETEParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/space/deleteById", {
    method: "DELETE",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** querySpaceById GET /space/querySpaceById */
export async function querySpaceByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.querySpaceByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseSpaceVO_>("/space/querySpaceById", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** querySpacePage GET /space/querySpacePage */
export async function querySpacePageUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.querySpacePageUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseSpacePageVO_>("/space/querySpacePage", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** updateById PUT /space/updateById */
export async function updateByIdUsingPut(
  body: API.SpaceUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/space/updateById", {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
