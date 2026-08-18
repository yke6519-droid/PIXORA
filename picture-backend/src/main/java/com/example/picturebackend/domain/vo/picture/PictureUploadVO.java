package com.example.picturebackend.domain.vo.picture;

import java.util.List;

import lombok.Data;

@Data
public class PictureUploadVO {
    private Integer totalCount;
    
    private Integer successCount;

    private Integer failCount;

    private List<PictureVO> successPictureList;

    private List<PictureUploadFailVO> failPictureList;
}
