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

    /** 批量抓图在前端超时前提前结束，留出少量时间返回已成功结果。 */
    public static final long BATCH_IMPORT_TIMEOUT_MILLIS = 58_000L;

    /** 单次图片来源请求和远程图片下载的默认超时时间。 */
    public static final int IMAGE_REQUEST_TIMEOUT_MILLIS = 20_000;
}
