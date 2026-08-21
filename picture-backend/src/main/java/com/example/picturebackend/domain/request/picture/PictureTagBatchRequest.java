package com.example.picturebackend.domain.request.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/** 批量绑定或移除图片标签请求体。 */
@Data
public class PictureTagBatchRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 图片所属空间。 */
    private Long spaceId;

    /** 待操作的图片 id。 */
    private List<Long> pictureIds;

    /** 待操作的标签 id。 */
    private List<Long> tagIds;
}
