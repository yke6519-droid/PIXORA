package com.example.picturebackend.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 公共图库主题表。
 *
 * <p>产品层称为“主题”，数据库沿用现有代码中的 category 命名。
 * 第一版只服务公共图库，不承担个人空间的自定义整理能力。</p>
 */
@Data
@TableName("category")
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 使用项目现有的雪花 ID 策略。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 主题展示名称。 */
    private String categoryName;

    /** 公共图库中的展示顺序，数值越小越靠前。 */
    private Integer sortOrder;

    /** 是否为系统主题；“未分类”属于系统主题。 */
    private Integer isSystem;

    private Date createTime;

    private Date updateTime;
}
