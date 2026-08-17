package com.example.picturebackend.domain.request.notification;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建通知的后端内部请求对象，不作为 Controller 入参。
 */
@Data
public class NotificationCreateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 通知类型，由 NotificationConstant 统一管理。 */
    private String type;
    /** 接收通知的用户 id。 */
    private Long userId;
    /** 关联业务数据 id。 */
    private Long bizId;
    /** 通知标题快照。 */
    private String title;
    /** 通知内容快照。 */
    private String content;
}
