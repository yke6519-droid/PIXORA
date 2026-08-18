// @ts-ignore
/* eslint-disable */
import request from "../plugin/request";

/** 此处后端没有提供注释 PUT /picture/adminCheckPicture */
export async function adminCheckPicture(
  body: API.AdminCheckPictureRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/picture/adminCheckPicture", {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 PUT /picture/adminCheckPictureBatch */
export async function adminCheckPictureBatch(
  body: API.AdminCheckPictureBatchRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/picture/adminCheckPictureBatch", {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /picture/adminFetchPictureBatch */
export async function adminFetchPictureBatch(
  body: API.PictureUploadByBatchRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePictureListVO>(
    "/picture/adminFetchPictureBatch",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      data: body,
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 DELETE /picture/deletePicture */
export async function deletePicture(
  body: API.PictureUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/picture/deletePicture", {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /picture/editPicture */
export async function editPicture(
  body: API.PictureUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/picture/editPicture", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /picture/getPictureById */
export async function getPictureById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getPictureByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePictureVO>("/picture/getPictureById", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /picture/queryAll */
export async function queryAll(
  body: API.PictureQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseIPagePictureVO>("/picture/queryAll", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /picture/queryPicturePage */
export async function queryPicturePage(
  body: API.PictureQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePicturePageVO>("/picture/queryPicturePage", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /picture/queryPicturePageCache */
export async function queryPicturePageCache(
  body: API.PictureQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePicturePageVO>(
    "/picture/queryPicturePageCache",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      data: body,
      ...(options || {}),
    }
  );
}

/** 重新上传：后端通过 multipart 的 file/url 和 RequestParam 元信息接收请求。 */
export async function reloadPicture(
  params: API.reloadPictureParams,
  body: {},
  file?: File,
  options?: { [key: string]: any },
) {
  const formData = new FormData()
  if (file) formData.append('file', file)

  Object.entries({ ...params, ...body }).forEach(([key, value]) => {
    if (value === undefined || value === null) return
    if (Array.isArray(value)) {
      value.forEach((item) => formData.append(key, String(item)))
      return
    }
    formData.append(key, String(value))
  })

  return request<API.BaseResponsePictureVO>('/picture/reloadPicture', {
    method: 'POST',
    data: formData,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /picture/tag_category */
export async function listPictureCategory(options?: { [key: string]: any }) {
  return request<API.BaseResponsePictureTagCategory>("/picture/tag_category", {
    method: "GET",
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /picture/updatePicture */
export async function updatePicture(
  body: API.PictureUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/picture/updatePicture", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /picture/uploadPic */
export async function uploadPic(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.uploadPicParams,
  body: {},
  fileList?: File[],
  options?: { [key: string]: any }
) {
  const formData = new FormData();

  if (fileList) {
    fileList.forEach((f) => formData.append("fileList", f || ""));
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

  return request<API.BaseResponsePictureUploadVO>("/picture/uploadPic", {
    method: "POST",
    params: {
      ...params,
    },
    data: formData,
    ...(options || {}),
  });
}
