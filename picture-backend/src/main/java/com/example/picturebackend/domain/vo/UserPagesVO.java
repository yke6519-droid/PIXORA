package com.example.picturebackend.domain.vo;

import com.example.picturebackend.domain.po.User;
import lombok.Data;

import java.util.List;

@Data
public class UserPagesVO {
    // 分页查到的用户列表
    private List<User> userList;
    // 总页数
    private Long totalSize;
}
