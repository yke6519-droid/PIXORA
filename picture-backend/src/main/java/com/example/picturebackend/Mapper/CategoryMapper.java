package com.example.picturebackend.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.picturebackend.domain.po.Category;
import org.apache.ibatis.annotations.Mapper;

/** 公共主题的数据访问接口。 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
