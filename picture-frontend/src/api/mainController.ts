// @ts-ignore
/* eslint-disable */
import request from "../plugin/request";

/** 此处后端没有提供注释 GET /health/isSuccess */
export async function health(options?: { [key: string]: any }) {
  return request<API.BaseResponseString>("/health/isSuccess", {
    method: "GET",
    ...(options || {}),
  });
}
