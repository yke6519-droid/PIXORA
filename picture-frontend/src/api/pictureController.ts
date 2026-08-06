// @ts-ignore
/* eslint-disable */
import request from "../plugin/request";

/** adminCheckPicture PUT /picture/adminCheckPicture */
export async function adminCheckPictureUsingPut(
  body: API.AdminCheckPictureRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/picture/adminCheckPicture", {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** adminCheckPictureBatch PUT /picture/adminCheckPictureBatch */
export async function adminCheckPictureBatchUsingPut(
  body: API.AdminCheckPictureBatchRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/picture/adminCheckPictureBatch", {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** adminFetchPictureBatch POST /picture/adminFetchPictureBatch */
export async function adminFetchPictureBatchUsingPost(
  body: API.PictureUploadByBatchRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePictureListVO_>("/picture/adminFetchPictureBatch", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** deletePicture DELETE /picture/deletePicture */
export async function deletePictureUsingDelete(
  body: API.PictureUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/picture/deletePicture", {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** editPicture POST /picture/editPicture */
export async function editPictureUsingPost(
  body: API.PictureUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/picture/editPicture", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** getPictureById GET /picture/getPictureById */
export async function getPictureByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getPictureByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePictureVO_>("/picture/getPictureById", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** queryAll POST /picture/queryAll */
export async function queryAllUsingPost(
  body: API.PictureQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseIPagePicture_>("/picture/queryAll", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** queryPicturePage POST /picture/queryPicturePage */
export async function queryPicturePageUsingPost(
  body: API.PictureQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePicturePageVO_>("/picture/queryPicturePage", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** queryPicturePageCache POST /picture/queryPicturePageCache */
export async function queryPicturePageCacheUsingPost(
  body: API.PictureQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePicturePageVO_>(
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

/** listPictureCategory GET /picture/tag_category */
export async function listPictureCategoryUsingGet(options?: {
  [key: string]: any;
}) {
  return request<API.BaseResponsePictureTagCategory_>("/picture/tag_category", {
    method: "GET",
    ...(options || {}),
  });
}

/** updatePicture POST /picture/updatePicture */
export async function updatePictureUsingPost(
  body: API.PictureUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/picture/updatePicture", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** uploadPic POST /picture/uploadPic */
export async function uploadPicUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.uploadPicUsingPOSTParams,
  body: {},
  fileList?: File | File[],
  options?: { [key: string]: any }
) {
  const formData = new FormData();

  const files = Array.isArray(fileList)
    ? fileList
    : fileList
      ? [fileList]
      : [];
  files.forEach((file) => formData.append("fileList", file));

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

  return request<API.BaseResponseListPictureVO_>("/picture/uploadPic", {
    method: "POST",
    params: {
      ...params,
    },
    data: formData,
    ...(options || {}),
  });
}

/** reloadPicture POST /picture/reloadPicture */
export async function reloadPictureUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: {},
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

  return request<API.BaseResponsePictureVO_>("/picture/reloadPicture", {
    method: "POST",
    params: {
      ...params,
    },
    data: formData,
    ...(options || {}),
  });
}
