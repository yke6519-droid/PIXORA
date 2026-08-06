package com.example.picturebackend.domain.request.picture;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PictureUploadRequest implements Serializable {
    /**
     * 图片id 用于修改图片
     * 由于在本项目中 图片上传后会立即被传入数据库中 会产生一个id
     * 因此后续再传图片，是对当前id对应的图片的更新
     * 而不是重新插入一个图片 再去产生一个id
     */
    private Long id;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 图片分类
     */
    private String category;

    /**
     * 图片标签
     */
    private List<String> tags;

    /**
     * 图片简介
     */
    private String introduction;

    /**
     * 空间id
     */
    private Long spaceId;

    @TableField(exist = false)
    private static final long serialVersionUID = 3191241716373120793L;
}
