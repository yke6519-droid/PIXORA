// @ts-ignore
/* eslint-disable */
import request from "../plugin/request";

/** health GET /health/isSuccess */
export async function healthUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseString_>("/health/isSuccess", {
    method: "GET",
    ...(options || {}),
  });
}
