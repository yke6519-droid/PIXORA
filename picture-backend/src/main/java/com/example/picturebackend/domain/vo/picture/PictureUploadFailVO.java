package com.example.picturebackend.domain.vo.picture;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * PictureUploadFailVO
 */
@Data
@AllArgsConstructor
public class PictureUploadFailVO {
    // 失败图片索引
    // private Integer index;
    private Long size;
    // 图片名称
    private String fileName;
    // 失败消息
    private String message;
}
