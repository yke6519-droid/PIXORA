package com.example.picturebackend.domain.request.tag;

import lombok.Data;

import java.io.Serializable;

/** 修改空间标签名称请求体。 */
@Data
public class TagRenameRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 标签 id。 */
    private Long tagId;

    /** 新的标签展示名称。 */
    private String tagName;
}
