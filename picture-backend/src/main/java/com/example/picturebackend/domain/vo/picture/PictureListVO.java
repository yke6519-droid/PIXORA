package com.example.picturebackend.domain.vo.picture;

import java.util.List;

import lombok.Data;

/**
 * 用于批量拉取后，拿到拉取到的图片数据
 * 包含：图片类、目标拉取数量、成功拉取数量
 * PictureListVO
 */
@Data
public class PictureListVO {

    List<PictureVO> pictureList;

    Integer targetCount;

    Integer successCount;
    
}
