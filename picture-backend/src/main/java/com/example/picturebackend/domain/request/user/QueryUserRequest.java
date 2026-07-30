package com.example.picturebackend.domain.request.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class QueryUserRequest implements Serializable {
    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 要查询的用户id
     */
    private Long id;
}
