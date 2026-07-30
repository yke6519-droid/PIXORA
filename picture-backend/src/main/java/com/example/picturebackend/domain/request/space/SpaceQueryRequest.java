package com.example.picturebackend.domain.request.space;

import com.example.picturebackend.domain.request.PageRequest;
import lombok.Data;

@Data
public class SpaceQueryRequest extends PageRequest {
    /**
     * 空间id
     */
    private Long id;
    /**
     * 空间名
     */
    private String spaceName;
    /**
     * 空间等级
     */
    private Integer spaceLevel;
}
