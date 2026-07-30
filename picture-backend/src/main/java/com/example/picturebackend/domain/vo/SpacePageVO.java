package com.example.picturebackend.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class SpacePageVO {
    /**
     * 图片列表
     */
    private List<SpaceVO> spaceVOList;

    /**
     * 总记录数
     */
    private Long total;
}
