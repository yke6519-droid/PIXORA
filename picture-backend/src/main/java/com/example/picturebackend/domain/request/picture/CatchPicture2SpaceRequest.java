package com.example.picturebackend.domain.request.picture;

import lombok.Data;

/**
 * 从公共图库，复制图片到私人空间
 * CatchPicture2SpaceRequest
 */
@Data
public class CatchPicture2SpaceRequest {
    
    // targetPictureId: 目标图片id
    private Long targetPictureId;

    // targetSpaceId: 目标空间id
    private Long targetSpaceId;
}
