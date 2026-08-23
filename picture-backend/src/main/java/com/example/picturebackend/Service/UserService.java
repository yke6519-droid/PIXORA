package com.example.picturebackend.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.example.picturebackend.domain.po.User;
import com.example.picturebackend.domain.request.user.*;
import com.example.picturebackend.domain.vo.picture.UserPictureVO;
import com.example.picturebackend.domain.vo.space.UserSpaceVO;
import com.example.picturebackend.domain.vo.user.UserVO;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author chen
* @description 针对表【user】的数据库操作Service
* @createDate 2026-04-21 15:38:23
*/
public interface UserService extends IService<User> {

    String userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);

    UserVO getSaftyUser(User user);

    /** 构造图片创建者的公开展示信息。 */
    UserPictureVO getUserPictureVO(User user);

    /** 构造空间持有人的公开展示信息。 */
    UserSpaceVO getUserSpaceVO(User user);

    User getCurrentUser(HttpServletRequest request);

    Boolean userRegister(RegisterRequest registerRequest);

    boolean updateSelf(Long id, HttpServletRequest request, UpdateSelfRequest updateSelfRequest);

    boolean addUser(AddUserRequest addUserRequest);

    /**
     * 管理员统一删除用户入口。
     */
    boolean adminDeleteUsers(List<Long> userIds, User currentAdmin);

    IPage<User> queryPageByCondition(QueryPageRequest queryPageRequest);

    /**
     * 定义一个查询类，用于快速根据request生成queryWrapper
     */
    QueryWrapper<User> getQueryWrapper(QueryPageRequest queryPageRequest);

    Boolean adminCheckAvatar(AdminCheckAvatarRequest adminCheckAvatarRequest, User currentUser);

}
