package com.example.picturebackend.domain.po;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@TableName(value ="avatar_check")
@Data
public class AvatarCheck implements Serializable{
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     *新头像url
     */
    private String url;

    /**
     * 审核状态
     * 0 待审核
     * 1 审核通过
     * 2 审核失败
     */
    private Integer status;

    /**
     * 持有人id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createtime;

    /**
     * 更新时间
     */
    private Date updatetime;

    private String checkMessage;
}