package com.example.picturebackend.domain.request.picture;

import com.example.picturebackend.domain.request.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 图片分页查询请求体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PictureQueryRequest extends PageRequest {
    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    private Long id;
    /**
     * 图片名称（模糊搜索）
     */
    private String name;

    /**
     * 简介（模糊搜索）
     */
    private String introduction;

    /** 公共图库主题 id。 */
    private Long categoryId;

    /** 个人空间标签 id；阶段 4B 接入关联查询。 */
    private List<Long> tagIds;

    /**
     * 查询文本
     */
    private String searchText;

    /**
     * 审核状态
     */
    private Integer pictureCheck;

    /**
     * 用户id（管理员可按用户筛选）
     */
    private Long userId;

    /**
     * 查找空间
     */
    private Long spaceId;
}
