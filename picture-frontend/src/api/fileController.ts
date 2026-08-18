// @ts-ignore
/* eslint-disable */
import request from "../plugin/request";

/** 头像上传：后端使用 @RequestParam("avatar") 接收 multipart 文件。 */
export async function avatarUpload(file: File, options?: { [key: string]: any }) {
  const formData = new FormData()
  formData.append('avatar', file)

  return request<API.BaseResponseUploadAvatarVO>('/file/avatarUpload', {
    method: 'POST',
    data: formData,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /file/downloadFile */
export async function testDownloadFile(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.testDownloadFileParams,
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

/** 此处后端没有提供注释 POST /file/testUpload */
export async function testUploadFile(
  body: {},
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseString>("/file/testUpload", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
