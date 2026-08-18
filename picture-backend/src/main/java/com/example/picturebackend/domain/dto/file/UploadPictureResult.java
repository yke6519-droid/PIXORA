package com.example.picturebackend.domain.dto.file;

import lombok.Data;


@Data
public class UploadPictureResult {
    /**
     * 图片url
     */
    private String url;

    /**
     * 缩略图url
     */
    private String thumbnailUrl;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 图片体积
     */
    private Long picsize;

    /**
     * 图片宽度
     */
    private Integer picwidth;

    /**
     * 图片高度
     */
    private Integer picheight;

    /**
     * 图片宽高比例
     */
    private Double picscale;

    /**
     * 图片格式
     */
    private String picformat;

    /**
     * 上传cos的key
     */
    private String pictureKey;

    private String thumbnailKey;

    private String originalKey;
}
