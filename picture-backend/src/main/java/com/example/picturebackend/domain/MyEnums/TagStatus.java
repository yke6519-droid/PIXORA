package com.example.picturebackend.domain.MyEnums;

import lombok.Getter;

/**
 * 空间标签状态。
 *
 * <p>标签删除使用物理删除，状态字段只表达管理员停用或恢复，避免把两个生命周期概念混在一起。</p>
 */
@Getter
public enum TagStatus {
    /** 可以绑定新图片，也参与正常展示和筛选。 */
    ACTIVE(1, "可用"),
    
    /** 保留实体和历史关联，但不能绑定新图片，也不参与普通展示和筛选。 */
    DISABLED(0, "停用");

    private final Integer value;
    private final String text;

    TagStatus(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    /** 根据数据库中的数值状态查找枚举。 */
    public static TagStatus getByValue(Integer value) {
        for (TagStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return null;
    }
}
