// @ts-ignore
/* eslint-disable */
import request from "../plugin/request";

/** avatarUpload POST /file/avatarUpload */
export async function avatarUploadUsingPost(
  body: {},
  avatar?: File,
  options?: { [key: string]: any }
) {
  const formData = new FormData();

  if (avatar) {
    formData.append("avatar", avatar);
  }

  Object.keys(body).forEach((ele) => {
    const item = (body as any)[ele];

    if (item !== undefined && item !== null) {
      if (typeof item === "object" && !(item instanceof File)) {
        if (item instanceof Array) {
          item.forEach((f) => formData.append(ele, f || ""));
        } else {
          formData.append(
            ele,
            new Blob([JSON.stringify(item)], { type: "application/json" })
          );
        }
      } else {
        formData.append(ele, item);
      }
    }
  });

  return request<API.BaseResponseString_>("/file/avatarUpload", {
    method: "POST",
    data: formData,
    requestType: "form",
    ...(options || {}),
  });
}

/** testDownloadFile GET /file/downloadFile */
export async function testDownloadFileUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.testDownloadFileUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<any>("/file/downloadFile", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** testUploadFile POST /file/testUpload */
export async function testUploadFileUsingPost(
  body: {},
  file?: File,
  options?: { [key: string]: any }
) {
  const formData = new FormData();

  if (file) {
    formData.append("file", file);
  }

  Object.keys(body).forEach((ele) => {
    const item = (body as any)[ele];

    if (item !== undefined && item !== null) {
      if (typeof item === "object" && !(item instanceof File)) {
        if (item instanceof Array) {
          item.forEach((f) => formData.append(ele, f || ""));
        } else {
          formData.append(
            ele,
            new Blob([JSON.stringify(item)], { type: "application/json" })
          );
        }
      } else {
        formData.append(ele, item);
      }
    }
  });

  return request<API.BaseResponseString_>("/file/testUpload", {
    method: "POST",
    data: formData,
    requestType: "form",
    ...(options || {}),
  });
}
