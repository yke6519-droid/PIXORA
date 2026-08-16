package com.example.picturebackend.domain.request.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UpdateSelfRequest implements Serializable {
    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 用户名
     */
    private String username;

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
}
