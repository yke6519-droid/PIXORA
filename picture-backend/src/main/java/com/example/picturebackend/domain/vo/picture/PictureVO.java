package com.example.picturebackend.domain.vo.picture;

import com.example.picturebackend.domain.po.User;
import com.example.picturebackend.domain.vo.user.UserVO;

import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.util.Date;
import java.util.List;

@Data
public class PictureVO {
    private Long id;
    /**
     * 图片url
     */
    private String url;
    /**
     * 缩略图url
     */
    private String thumbnailUrl;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签数组
     * 与Picture实体类不同
     * 这里方便前端展示 所以用List
     */
    private List<String> tags;

    /**
     * 图片体积
     */
    private Long picsize;

    /**
     * 图片宽度
     */
    private Integer picwidth;


    /**
     * 图片高度
     */
    private Integer picheight;

    /**
     * 图片宽高比例
     */
    private Double picscale;

    /**
     * 图片格式
     */
    private String picformat;

    /**
     * 创建用户id
     */
    private Long userId;

    /**
     * 创建用户信息
     *
     */
    private UserVO createdUser;

    /**
     * 创建时间
     */
    private Date createtime;

    /**
     * 更新时间
     */
    private Date updatetime;

    /**
     * 图片审核状态
     */
    private Integer pictureCheck;

    /**
     * 审核人id
     */
    private Long checkAdminId;

    /**
     * 审核原因
     */
    private String checkMessage;

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 快速转换
     * 实体类转VO
     */
    public static PictureVO obj2VO(Object o) {
        PictureVO pictureVO = new PictureVO();
        BeanUtils.copyProperties(o, pictureVO);
        return pictureVO;
    }
}
