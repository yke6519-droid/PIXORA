package com.example.picturebackend.domain.po;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户私有空间表
 * @TableName space
 */
@TableName(value ="space")
@Data
public class Space {
    /**
     * 空间id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 空间名
     */
    private String spaceName;

    /**
     * 空间级别 0-普通版 1-专业版（对应的空间容量不同）
     */
    private Integer spaceLevel;

    /**
     * 空间容量上限
     */
    private Long maxSize;

    /**
     * 用户已占用的空间大小
     */
    private Long usedSize;

    /**
     * 空间图片最大数量
     */
    private Long maxCount;

    /**
     * 已存入图片数
     */
    private Long usedCount;

    /**
     * 持有人id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer isDelete;

}