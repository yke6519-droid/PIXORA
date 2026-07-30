package com.example.picturebackend.domain.request.space;

import lombok.Data;

import java.util.Date;

@Data
public class CreateSpaceRequest {
    /**
     * 空间名
     */
    private String spaceName;
}
