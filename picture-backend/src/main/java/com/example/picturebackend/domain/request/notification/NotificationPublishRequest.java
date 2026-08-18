package com.example.picturebackend.domain.request.notification;

import lombok.Data;

import java.io.Serializable;

/** 管理员发布系统通知请求。 */
@Data
public class NotificationPublishRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 通知类型；管理员公共发布端必须显式传系统公告类型。 */
    private String type;
    /** true 表示全体未删除用户，否则必须指定 userId。 */
    private Boolean allUsers;
    /** 指定接收通知的用户 id；全体发布时必须为空。 */
    private Long userId;
    /** 通知标题，非空且长度不超过 128。 */
    private String title;
    /** 通知内容，非空且长度不超过 1024。 */
    private String content;
}
