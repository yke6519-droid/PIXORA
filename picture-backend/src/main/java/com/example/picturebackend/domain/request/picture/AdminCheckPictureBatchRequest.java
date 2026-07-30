package com.example.picturebackend.domain.request.picture;

import lombok.Data;

import java.util.List;

@Data
public class AdminCheckPictureBatchRequest {
    /**
     * 审核图片的id
     */
    private List<Long> picIds;

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
