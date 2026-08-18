package com.example.picturebackend.domain.vo.notification;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户通知展示对象。
 */
@Data
public class NotificationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 通知 id。 */
    private Long id;

    /** 通知类型。 */
    private String type;

    /** 通知标题。 */
    private String title;

    /** 通知内容。 */
    private String content;

    /** 业务类型。 */
    private String bizType;

    /** 关联业务 id。 */
    private Long bizId;

    /** 关联业务名称；图片审核通知中表示图片名称。 */
    private String bizName;

    /** 已读时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date readTime;

    /** 创建时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;
}
