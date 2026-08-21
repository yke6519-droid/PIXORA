package com.example.picturebackend.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 图片与空间标签的关联记录。
 *
 * <p>解绑就是删除一条关联记录，删除标签实体时由标签模块在同一事务中清理关联。</p>
 */
@Data
@TableName("picture_tag")
public class PictureTag implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 关联记录自身的 ID，方便 MyBatis-Plus 使用通用 Mapper。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 图片 ID。 */
    private Long pictureId;

    /** 标签 ID。 */
    private Long tagId;

    /** 绑定时间。 */
    private Date createTime;
}
