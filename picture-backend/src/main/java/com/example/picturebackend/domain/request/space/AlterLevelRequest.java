package com.example.picturebackend.domain.request.space;

import lombok.Data;

@Data
public class AlterLevelRequest {
    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 修改等级
     */
    private Integer alterLevel;
}
