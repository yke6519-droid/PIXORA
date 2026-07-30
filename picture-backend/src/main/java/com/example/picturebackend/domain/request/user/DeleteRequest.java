package com.example.picturebackend.domain.request.user;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 删除请求体
 */
@Data
public class DeleteRequest implements Serializable {
    private static final long serialVersionUID = 3191241716373120793L;
    /**
     * 要删除的id
     */
    private long id;

    /**
     * 用于批量删除
     */
    private List<Long> ids;
}
