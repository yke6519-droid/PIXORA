package com.example.picturebackend.domain.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.example.picturebackend.domain.po.Space;
import com.example.picturebackend.domain.po.User;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.util.Date;

@Data
public class SpaceVO {
    /**
     * 空间id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 空间名
     */
    private String spaceName;

    /**
     * 空间级别 0-普通版 1-专业版（对应的空间容量不同）
     */
    private Integer spaceLevel;

    /**
     * 空间容量上限
     */
    private Long maxSize;

    /**
     * 用户已占用的空间大小
     */
    private Long usedSize;

    /**
     * 空间图片最大数量
     */
    private Long maxCount;

    /**
     * 已存入图片数
     */
    private Long usedCount;

    /**
     * 持有人id
     */
    private Long userId;

    /**
     * 持有人信息
     * 需要脱敏
     */
    private User createdUser;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 实体转VO
     * @param space
     * @return
     */
    public SpaceVO Space2SpaceVO(Space space){
        if (space == null){
            return  null;
        }
        SpaceVO spaceVO = new SpaceVO();
        BeanUtils.copyProperties(space,spaceVO);
        return spaceVO;
    }

    /**
     * VO转实体
     * @param spaceVO
     * @return
     */
    public Space SpaceVO2Space(SpaceVO spaceVO){
        if (spaceVO == null){
            return null;
        }
        Space space = new Space();
        BeanUtils.copyProperties(spaceVO,space);
        return space;
    }
}
