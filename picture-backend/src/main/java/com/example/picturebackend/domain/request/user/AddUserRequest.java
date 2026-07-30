package com.example.picturebackend.domain.request.user;

import lombok.Data;
import java.io.Serializable;

@Data
public class AddUserRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 用户名
     */
    private String username = "无名";

    /**
     * 用户账号
     */
    private String useraccount;

    /**
     * 性别
     */
    private Integer gender=0;

    /**
     * 电话号码
     */
    private String phone="12345678901";

    /**
     * 用户状态
     */
    private String userstatus;
}
