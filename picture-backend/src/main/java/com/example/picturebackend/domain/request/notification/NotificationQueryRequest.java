package com.example.picturebackend.domain.request.notification;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户通知分页查询请求。
 */
@Data
public class NotificationQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页码。 */
    private Long current = 1L;

    /** 每页条数。 */
    private Long pageSize = 10L;

    /** 是否仅查询未读通知。 */
    private Boolean unreadOnly = false;
}
