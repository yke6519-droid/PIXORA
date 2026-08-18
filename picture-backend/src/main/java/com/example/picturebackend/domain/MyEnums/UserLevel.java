package com.example.picturebackend.domain.MyEnums;

import cn.hutool.core.util.StrUtil;
import com.example.picturebackend.Exception.ErrorCode;
import com.example.picturebackend.Exception.ThrowExceptionUtils;
import lombok.Getter;

/**
 * 用户等级/角色枚举。
 */
@Getter
public enum UserLevel {
    NORMAL_USER("普通用户", "user"),
    ADMIN("管理员", "admin"),
    VIP_USER("会员用户", "vip");

    private final String text;
    private final String value;

    UserLevel(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据角色值获取枚举。
     */
    public static UserLevel getEnumByValue(String value) {
        ThrowExceptionUtils.throwIF(
                StrUtil.isBlank(value),
                ErrorCode.PARAMS_ERROR,
                "用户等级不能为空"
        );
        for (UserLevel userLevel : UserLevel.values()) {
            if (userLevel.getValue().equals(value)) {
                return userLevel;
            }
        }
        return null;
    }
}
