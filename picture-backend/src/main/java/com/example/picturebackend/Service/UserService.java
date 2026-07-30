package com.example.picturebackend.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.example.picturebackend.domain.po.User;
import com.example.picturebackend.domain.request.user.*;

import jakarta.servlet.http.HttpServletRequest;

/**
* @author chen
* @description 针对表【user】的数据库操作Service
* @createDate 2026-04-21 15:38:23
*/
public interface UserService extends IService<User> {

    String userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);

    User getSaftyUser(User user);

    User getCurrentUser(HttpServletRequest request);

    Boolean userRegister(RegisterRequest registerRequest);

    String passwordEncrypt(String password);

    boolean updateSelf(Long id, HttpServletRequest request, UpdateSelfRequest updateSelfRequest);

    boolean addUser(AddUserRequest addUserRequest);

    IPage<User> queryPageByCondition(QueryPageRequest queryPageRequest);

    /**
     * 定义一个查询类，用于快速根据request生成queryWrapper
     */
    QueryWrapper<User> getQueryWrapper(QueryPageRequest queryPageRequest);
}
