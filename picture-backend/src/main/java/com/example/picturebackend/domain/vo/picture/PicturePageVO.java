package com.example.picturebackend.domain.vo.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 图片分页返回VO
 */
@Data
public class PicturePageVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 图片列表
     */
    private List<PictureVO> pictureList;

    /**
     * 总记录数
     */
    private Long total;
}
