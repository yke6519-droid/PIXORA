package com.example.picturebackend.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户通知表。
 *
 * <p>当前仅承载头像审核结果通知，后续可通过 type、bizType 和 bizId 扩展其他通知来源。</p>
 */
@Data
@TableName("user_notification")
public class UserNotification implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 通知 id，使用项目现有的雪花 ID 策略生成。
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 接收通知的用户 id。
     */
    private Long userId;

    /**
     * 通知类型，例如 AVATAR_REVIEW_RESULT。
     */
    private String type;

    /**
     * 通知标题快照。
     */
    private String title;

    /**
     * 通知内容快照。
     */
    private String content;

    /**
     * 业务类型，例如 AVATAR_CHECK。
     */
    private String bizType;

    /**
     * 关联业务数据 id，例如 AvatarCheck.id。
     */
    private Long bizId;

    /**
     * 已读时间，为 null 表示未读。
     */
    private Date readTime;

    /**
     * 逻辑删除：0-未删除，1-已删除。
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 创建时间。
     */
    private Date createTime;

    /**
     * 更新时间。
     */
    private Date updateTime;
}
