package com.example.picturebackend.constant;

/**
 * 通知类型与业务类型常量。
 */
public final class NotificationConstant {
    private NotificationConstant() {
    }

    /** 系统公告通知类型。 */
    public static final String TYPE_SYSTEM_ANNOUNCEMENT = "SYSTEM_ANNOUNCEMENT";
    /** 头像审核结果通知类型。 */
    public static final String TYPE_AVATAR_REVIEW_RESULT = "AVATAR_REVIEW_RESULT";
    /** 图片审核结果通知类型。 */
    public static final String TYPE_PICTURE_REVIEW_RESULT = "PICTURE_REVIEW_RESULT";
    /** 系统公告业务类型。 */
    public static final String BIZ_TYPE_SYSTEM_ANNOUNCEMENT = "SYSTEM_ANNOUNCEMENT";
    /** 头像审核业务类型。 */
    public static final String BIZ_TYPE_AVATAR_CHECK = "AVATAR_CHECK";
    /** 图片审核业务类型。 */
    public static final String BIZ_TYPE_PICTURE_CHECK = "PICTURE_CHECK";
    /** 头像审核通过通知标题。 */
    public static final String AVATAR_REVIEW_APPROVED_TITLE = "头像审核通过";
    /** 头像审核通过通知内容。 */
    public static final String AVATAR_REVIEW_APPROVED_CONTENT = "你的新头像已审核通过，现已生效。";
    /** 头像审核拒绝通知标题。 */
    public static final String AVATAR_REVIEW_REJECTED_TITLE = "头像审核未通过";
    /** 头像审核拒绝通知内容前缀。 */
    public static final String AVATAR_REVIEW_REJECTED_CONTENT_PREFIX = "你的头像审核未通过，原因：";
    /** 图片审核通过通知标题。 */
    public static final String PICTURE_REVIEW_APPROVED_TITLE = "图片审核通过";
    /** 图片审核通过通知内容。 */
    public static final String PICTURE_REVIEW_APPROVED_CONTENT = "你的图片已审核通过，现已发布。";
    /** 图片审核拒绝通知标题。 */
    public static final String PICTURE_REVIEW_REJECTED_TITLE = "图片审核未通过";
    /** 图片审核拒绝通知内容前缀。 */
    public static final String PICTURE_REVIEW_REJECTED_CONTENT_PREFIX = "你的图片审核未通过，原因：";
}
