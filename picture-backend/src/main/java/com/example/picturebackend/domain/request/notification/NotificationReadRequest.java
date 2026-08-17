package com.example.picturebackend.domain.request.notification;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/** 当前用户批量标记已读通知请求。 */
@Data
public class NotificationReadRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 待标记通知 id，服务端最多处理 100 个。 */
    private List<Long> ids;
}
