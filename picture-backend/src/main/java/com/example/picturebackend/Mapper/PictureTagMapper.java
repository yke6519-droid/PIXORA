package com.example.picturebackend.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.picturebackend.domain.po.PictureTag;
import org.apache.ibatis.annotations.Mapper;

/** 图片与标签关联关系的数据访问接口。 */
@Mapper
public interface PictureTagMapper extends BaseMapper<PictureTag> {
}
