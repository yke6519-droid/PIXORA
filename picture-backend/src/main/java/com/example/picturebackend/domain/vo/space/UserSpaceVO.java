package com.example.picturebackend.domain.vo.space;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间持有人的公开展示信息。
 *
 * <p>空间详情和空间运营列表只能使用本对象，避免返回持有人的隐私资料。</p>
 */
@Data
public class UserSpaceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 id，用于前端关联、跳转和后续审计。 */
    private Long id;

    /** 用户昵称。 */
    private String username;

    /** 已审核通过的头像地址。 */
    private String avatarurl;
}
