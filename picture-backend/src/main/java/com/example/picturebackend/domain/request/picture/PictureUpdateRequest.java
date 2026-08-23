package com.example.picturebackend.domain.request.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片更新请求体
 */
@Data
public class PictureUpdateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 图片id
     */
    private Long id;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /** 公共图库主题 id；个人空间图片不使用主题。 */
    private Long categoryId;

    /**
     * 图片空间
     */
    private Long spaceId;
}
