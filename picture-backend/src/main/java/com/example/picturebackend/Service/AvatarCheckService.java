package com.example.picturebackend.Service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.example.picturebackend.domain.po.AvatarCheck;
import com.example.picturebackend.domain.po.User;
import com.example.picturebackend.domain.request.user.AdminCheckAvatarRequest;


/**
* @author chen
* @description 针对表【avatar_check(头像审核数据库)】的数据库操作Service
* @createDate 2026-08-15 19:32:59
*/
public interface AvatarCheckService extends IService<AvatarCheck> {

    // Boolean adminCheckAvatar(AdminCheckAvatarRequest adminCheckAvatarRequest, User currentUser);

}
