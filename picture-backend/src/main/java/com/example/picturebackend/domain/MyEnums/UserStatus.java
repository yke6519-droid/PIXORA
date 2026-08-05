package com.example.picturebackend.domain.MyEnums;

import lombok.Getter;

/**
 * 用户账户状态枚举。
 */
@Getter
public enum UserStatus {
    NORMAL("正常", 0),
    BANNED("封禁", 1);

    private final String text;
    private final Integer value;

    UserStatus(String text, Integer value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据账户状态值获取枚举；未知值返回 null，由请求层统一报参数错误。
     */
    public static UserStatus getEnumByValue(Integer value) {
        for (UserStatus userStatus : UserStatus.values()) {
            if (userStatus.getValue().equals(value)) {
                return userStatus;
            }
        }
        return null;
    }
}
