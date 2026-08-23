package com.example.picturebackend.domain.request.picture;

import lombok.Data;

import java.util.List;

/** 管理员批量设置公共图库图片主题的请求参数。 */
@Data
public class PictureCategoryBatchRequest {

    /** 待设置主题的图片 id。 */
    private List<Long> pictureIds;

    /** 要设置的公共主题 id。 */
    private Long categoryId;
}
