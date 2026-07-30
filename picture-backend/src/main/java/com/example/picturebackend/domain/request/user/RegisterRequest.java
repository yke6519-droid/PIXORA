package com.example.picturebackend.domain.request.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class RegisterRequest implements Serializable {
    private static final long serialVersionUID = 3191241716373120793L;
    /**
     * 用户名
     */
    private String username;

    /**
     * 用户账号
     */
    private String useraccount;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 用户密码
     */
    private String userpassword;

    /**
     * 确认密码
     */
    private String reUserPassword;

    /**
     * 电话号码
     */
    private String phone;
}
