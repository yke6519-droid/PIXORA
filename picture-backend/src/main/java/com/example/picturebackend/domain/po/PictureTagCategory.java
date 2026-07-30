package com.example.picturebackend.domain.po;

import lombok.Data;

import java.util.List;

@Data
public class PictureTagCategory {
    private List<String> tags;
    private List<String> categorys;
}
