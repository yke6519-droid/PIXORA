package com.example.picturebackend.domain.vo.user;

import lombok.Data;

import java.util.List;

@Data
public class UserPagesVO {
    // 分页查到的用户列表
    private List<UserVO> userList;
    // 总页数
    private Long totalSize;
}
