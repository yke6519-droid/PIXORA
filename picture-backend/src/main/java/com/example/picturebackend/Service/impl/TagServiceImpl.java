package com.example.picturebackend.Service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.picturebackend.Service.TagService;
import com.example.picturebackend.Mapper.TagMapper;
import com.example.picturebackend.domain.po.Tag;
import org.springframework.stereotype.Service;

/**
* @author chen
* @description 针对表【tag(图片标签表)】的数据库操作Service实现
* @createDate 2026-04-28 18:35:09
*/
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
    implements TagService{

}




