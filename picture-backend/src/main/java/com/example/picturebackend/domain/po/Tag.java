package com.example.picturebackend.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 空间标签表。
 *
 * <p>标签属于空间而不是用户；管理员通过 status 完成停用和恢复，
 * 用户删除标签实体时执行物理删除。</p>
 *
 * @TableName tag
 */
@TableName(value ="tag")
@Data
public class Tag implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 标签所属空间。 */
    private Long spaceId;

    /** 标签展示名称，保存去除首尾空格后的结果。 */
    private String tagName;

    /** 去除首尾空格并统一大小写后的判重值。 */
    private String normalizedName;

    /** 标签状态：1-可用，0-停用。 */
    private Integer status;

    /** 创建该标签的用户，只记录来源，不作为空间权限依据。 */
    private Long createdBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 3191241716373120793L;
}
