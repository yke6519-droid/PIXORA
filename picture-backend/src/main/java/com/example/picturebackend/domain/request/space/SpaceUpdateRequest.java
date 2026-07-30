package com.example.picturebackend.domain.request.space;

import lombok.Data;

@Data
public class SpaceUpdateRequest {
    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 修改名称
     */
    private String updatedName;
}
