package com.example.picturebackend.domain.po;


import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import lombok.Data;

/**
 *
 * @TableName user
 */
@TableName(value ="user")
@Data
public class User implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户账号
     */
    private String useraccount;

    /**
     * 头像
     */
    private String avatarurl;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 用户密码
     */
    private String userpassword;

    /**
     * 电话号码
     */
    private String phone;

    /**
     * 电子邮箱
     */
    private String email;

    /**
     * 创建时间
     */
    private LocalDateTime createtime;

    /**
     * 更新时间
     */
    private LocalDateTime updatetime;

    /**
     * 所属空间id
     */
    private Long spaceId;

    /**
     * 个人简介
     */
    private String profile;

    /**
     * 用户等级
     */
    @TableField("userLevel")
    private String userLevel;

    /**
     * 账户状态：0-正常，1-封禁。
     */
    @TableField("userStatus")
    private Integer userStatus;

    public Integer getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(Integer userStatus) {
        this.userStatus = userStatus;
    }

    /**
     * 是否删除
     */
    @TableLogic
    @TableField("isDelete")
    private Integer isdelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 3191241716373120793L;
}
