package com.example.picturebackend.domain.request.picture;

import lombok.Data;

@Data
public class Save2SpaceRequest {
    private Long spaceId;

    private Long pictureId;
}
