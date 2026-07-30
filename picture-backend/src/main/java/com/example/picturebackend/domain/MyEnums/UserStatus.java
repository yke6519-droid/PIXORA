package com.example.picturebackend.domain.MyEnums;

import cn.hutool.core.util.StrUtil;
import com.example.picturebackend.Exception.ErrorCode;
import com.example.picturebackend.Exception.ThrowExceptionUtils;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public enum UserStatus {
    NORMAL_USER("普通用户","user"),
    ADMIN("管理员","admin"),
    VIP_USER("会员用户","vip");

    private final String text;
    private final String value;

    UserStatus(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据value值获取到整个枚举类，方便后续校验使用
     * @param value
     * @return
     */
    public static UserStatus getEnumByValue(String value){
        ThrowExceptionUtils.throwIF(
                StrUtil.isBlank(value),
                ErrorCode.PARAMS_ERROR,
                "参数为空"
        );
        for (UserStatus userStatus: UserStatus.values()){
            if (userStatus.getValue().equals(value)){
                return userStatus;
            }
        }
        return null;
    }
}
