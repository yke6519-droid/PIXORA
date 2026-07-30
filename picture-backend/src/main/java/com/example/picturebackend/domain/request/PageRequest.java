package com.example.picturebackend.domain.request;

import lombok.Data;

/**
 * 分页请求体
 */
@Data
public class PageRequest {
    /**
     * 当前页号
     */
    private Integer current;
    /**
     * 页面大小
     */
    private Integer pageSize;
    /**
     * 排序字段
     */
    private String sortFiled = "createTime";
    /**
     * 排序规则
     */
    private String sortOrder = "descend";
}
