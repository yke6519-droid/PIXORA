package com.example.picturebackend.domain.request.user;

import lombok.Data;

@Data
public class QueryPageRequest {
    /**
     * 当前页
     */
    private Integer current;
    /**
     * 页面大小
     */
    private Integer size;
    /**
     * 排序字段
     */
    private String sortField;
    /**
     * 排序顺序
     */
    private String sortOrder = "ascend";
    /**
     * id
     */
    private Long id;
    /**
     * 查询条件：用户名
     */
    private String queryUsername;
    /**
     * 查询条件：账号
     */
    private String queryUserAccount;
    /**
     * 用户状态
     */
    private String userStatus;
    /**
     * 用户简介
     */
    private String profile;

    /**
     * 用户性别
     */
    private Integer gender;
}
