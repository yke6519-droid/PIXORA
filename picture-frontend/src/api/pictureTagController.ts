import request from "../plugin/request";

/** 批量给个人空间图片添加标签。 */
export async function addPictureTags(
  body: API.PictureTagBatchRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean>("/pictureTag/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 批量移除个人空间图片标签。 */
export async function removePictureTags(
  body: API.PictureTagBatchRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean>("/pictureTag/remove", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
