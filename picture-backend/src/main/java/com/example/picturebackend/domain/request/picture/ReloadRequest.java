package com.example.picturebackend.domain.request.picture;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.List;

@Data
public class ReloadRequest {
    private Long id;

    private String name;

    private String category;

    private List<String> tags;

    private String introduction;
}
