package com.example.picturebackend.domain.vo.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 管理员头像审核列表展示对象。
 * 只返回审核页面需要的用户脱敏信息和头像审核字段。
 */
@Data
public class AvatarReviewVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String username;

    private String useraccount;

    /** 待审核的新头像地址。 */
    private String avatarUrl;

    /** 0 待审核，1 审核通过，2 审核失败。 */
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date submittedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date reviewedAt;

    private String checkMessage;
}
