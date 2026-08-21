package com.example.picturebackend.domain.request.tag;

import lombok.Data;

import java.io.Serializable;

/** 创建空间标签请求体。 */
@Data
public class TagCreateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 标签所属空间。 */
    private Long spaceId;

    /** 标签展示名称。 */
    private String tagName;
}
