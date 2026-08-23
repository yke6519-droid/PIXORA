package com.example.picturebackend.domain.request.picture;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;

/**
 * 批量导入图片请求体
 */
@Data
public class PictureUploadByBatchRequest implements Serializable {
    /**
     * 爬取搜索词
     */
    private String searchText;
    /**
     * 爬取数量
     */
    private Integer count = 10;
    /**
     * 创建图片的名字前缀
     */
    private String name;
    /** 公共图库主题 id。 */
    private Long categoryId;

    @TableField(exist = false)
    private static final long serialVersionUID = 3191241716373120793L;
}
