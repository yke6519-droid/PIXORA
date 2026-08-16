package com.example.picturebackend.domain.request.user;

import lombok.Data;

@Data
public class AdminCheckAvatarRequest {
    /**
     * 用户的id，一个用户id对应一个新头像
     */
    private Long userId;

    /**
     * 审核后状态
     */
    private Integer checkResult;

    /**
     * 审核原因
     * 默认通过，若为失败状态则从前端获取到原因
     */
    private String checkMessage;
}
