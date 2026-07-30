package com.example.picturebackend.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 图库表
 * @TableName picture
 */
@TableName(value ="picture")
@Data
public class Picture implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 图片url
     */
    private String url;

    /**
     * 缩略图url
     */
    private String thumbnailUrl;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签JSON数组
     */
    private String tags;

    /**
     * 图片体积
     */
    private Long picsize;

    /**
     * 图片宽度
     */
    private Integer picwidth;

    /**
     * 图片高度
     */
    private Integer picheight;

    /**
     * 图片宽高比例
     */
    private Double picscale;

    /**
     * 图片格式
     */
    private String picformat;

    /**
     * 创建用户id
     */
    private Long userid;

    /**
     * 创建时间
     */
    private Date createtime;

    /**
     * 更新时间
     */
    private Date updatetime;

    /**
     * 编辑时间
     */
    private Date edittime;

    /**
     * 审核状态
     */
    private Integer pictureCheck;

    /**
     * 审核人id
     */
    private Long checkAdminId;

    /**
     * 审核时间
     */
    private Date checkTime;

    /**
     * 审核原因
     */
    private String checkMessage;

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 是否删除 0：未删 1：删除
     */
    @TableLogic
    private Integer isdelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 3191241716373120793L;

}