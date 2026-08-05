package com.example.picturebackend.domain.request.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class UpdateUserRequest implements Serializable {
    private static final long serialVersionUID = 3191241716373120793L;
    /**
     * 更新用户id
     */
    private Long id;
    /**
     * 用户名
     */
    private String username;

    /**
     * 头像
     */
    private String avatarurl;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 电话号码
     */
    private String phone;

    /**
     * 电子邮箱
     */
    private String email;

    /**
     * 个人简介
     */
    private String profile;

    /**
     * 用户等级：user、admin、vip。
     */
    private String userLevel;

    /**
     * 账户状态：0-正常，1-封禁。
     */
    private Integer accountStatus;
}
