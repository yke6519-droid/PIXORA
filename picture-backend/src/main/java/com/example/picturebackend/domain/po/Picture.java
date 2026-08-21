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

    /** 旧的自由文本分类，阶段 6 清理；新主题链路使用 categoryId。 */
    private String category;

    /** 新公共主题 ID；个人空间图片允许为空。 */
    private Long categoryId;

    /**
     * 旧的 JSON 标签数组，阶段 6 清理。
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
     * 图片id来源 - 默认为null
     * sourcePictureId
     * 用于判断是否已经保存过该图片该空间内
     * 防止用户同一张图片保存多次
     */
    private Long sourcePictureId=null;

    /**
     * WebP 正式图片
     */
    private String pictureKey;
    
    /**
     * 缩略图
     */
    private String thumbnailKey;
    
    /**
     * 原始上传文件
     */
    private String originalKey; 

    /**
     * 是否删除 0：未删 1：删除
     */
    @TableLogic
    private Integer isdelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 3191241716373120793L;

}
