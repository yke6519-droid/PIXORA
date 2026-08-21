package com.example.picturebackend.domain.request.tag;

import lombok.Data;

import java.io.Serializable;

/** 按标签 id 操作的请求体。 */
@Data
public class TagIdRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 标签 id。 */
    private Long tagId;
}
