package com.example.picturebackend.domain.vo.user;

import lombok.Data;

@Data
public class UploadAvatarVO {
    /**
     * 上传结果消息
     */
    private String message = "头像已上传，待管理员审核后更新";

    /**
     * 当前新头像的审核状态
     * 默认为待审核
     * 管理员上传为1 已审核
     */
    private Integer status = 0;

    /**
     * 新URL
     * 默认为空
     */
    private String newURL = null;
}
