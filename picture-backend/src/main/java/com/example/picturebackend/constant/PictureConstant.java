package com.example.picturebackend.constant;

import lombok.Data;

@Data
public class PictureConstant {
    public static final Integer CHECK_AWAIT = 0;
    public static final Integer CHECK_PASS = 1;
    public static final Integer CHECK_REFUSE = 2;

    /**
     * 单张图片最大 5MB，本地文件和网络图片统一使用该上限。
     */
    public static final long MAX_PICTURE_SIZE_BYTES = 5L * 1024 * 1024;
}
