package com.example.picturebackend.domain.request.notification;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/** 当前用户批量删除通知请求。 */
@Data
public class NotificationDeleteRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    /** 待删除通知 id，原始请求最多 100 个，服务端按去重后的 id 执行删除。 */
    private List<Long> ids;
}
