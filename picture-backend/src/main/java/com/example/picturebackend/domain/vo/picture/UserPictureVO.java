package com.example.picturebackend.domain.vo.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片创建者的公开展示信息。
 *
 * <p>图片列表和详情页只能使用本对象，避免账号、手机号、邮箱等资料随图片接口返回。</p>
 */
@Data
public class UserPictureVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 id，用于前端关联、跳转和后续审计。 */
    private Long id;

    /** 用户昵称。 */
    private String username;

    /** 已审核通过的头像地址。 */
    private String avatarurl;
}
