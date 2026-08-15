// @ts-ignore
/* eslint-disable */
import request from "../plugin/request";

/** 此处后端没有提供注释 PUT /space/alterLevelById */
export async function alterLevelById(
  body: API.AlterLevelRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/space/alterLevelById", {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /space/createSpace */
export async function createSpace(
  body: API.CreateSpaceRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseSpace>("/space/createSpace", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 DELETE /space/deleteById */
export async function deleteById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/space/deleteById", {
    method: "DELETE",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /space/querySpaceById */
export async function querySpaceById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.querySpaceByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseSpaceVO>("/space/querySpaceById", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /space/querySpacePage */
export async function querySpacePage(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.querySpacePageParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseSpacePageVO>("/space/querySpacePage", {
    method: "GET",
    params: {
      ...params,
      spaceQueryRequest: undefined,
      ...params["spaceQueryRequest"],
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 PUT /space/updateById */
export async function updateById(
  body: API.SpaceUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/space/updateById", {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
