package com.example.picturebackend.Service.impl;

import com.example.picturebackend.Exception.ErrorCode;
import com.example.picturebackend.Exception.ThrowExceptionUtils;
import com.example.picturebackend.Mapper.AvatarCheckMapper;
import com.example.picturebackend.Service.AvatarCheckService;
import com.example.picturebackend.Service.UserService;
import com.example.picturebackend.domain.po.AvatarCheck;
import com.example.picturebackend.domain.po.User;
import com.example.picturebackend.domain.request.user.AdminCheckAvatarRequest;

import jakarta.annotation.Resource;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
* @author chen
* @description 针对表【avatar_check(头像审核数据库)】的数据库操作Service实现
* @createDate 2026-08-15 19:32:59
*/
@Service
public class AvatarCheckServiceImpl extends ServiceImpl<AvatarCheckMapper, AvatarCheck>
    implements AvatarCheckService{
    
    // @Resource
    // private UserService userService;
    // @Override
    // public Boolean adminCheckAvatar(AdminCheckAvatarRequest adminCheckAvatarRequest, User currentUser){
        
    //     String checkMessage = adminCheckAvatarRequest.getCheckMessage();
    //     Integer checkResult = adminCheckAvatarRequest.getCheckResult();
    //     Long userId = adminCheckAvatarRequest.getUserId();



    //     // 拿到该用户对应的审核记录 - 最近一次的
    //     AvatarCheck avatarCheck = this.getOne(new QueryWrapper<AvatarCheck>()
    //                     .eq("userId", userId)
    //                     .orderByDesc("updateTime"));

    //     if (checkResult == 1) {
    //         ThrowExceptionUtils.throwIF(avatarCheck.getStatus().equals(checkResult), 
    //             ErrorCode.PARAMS_ERROR,"该头像已经是审核通过状态"
    //         );
    //         // 审核通过，修改状态并更新
    //         avatarCheck.setStatus(checkResult);
    //         avatarCheck.setCheckMessage("审核通过~");
    //         this.updateById(avatarCheck);
    //         // 修改用户头像
    //         User user = userService.getById(userId);
    //         user.setAvatarurl(avatarCheck.getUrl());
    //         userService.updateById(user);

    //     }else if (checkResult == 2) {
    //         ThrowExceptionUtils.throwIF(avatarCheck.getStatus().equals(checkResult), 
    //             ErrorCode.PARAMS_ERROR,"该头像已经是审核拒绝状态"
    //         );
    //         // 审核不通过
    //         avatarCheck.setStatus(checkResult);
    //         avatarCheck.setCheckMessage(checkMessage);
    //         this.updateById(avatarCheck);
    //         // 用户头像保持原样
    //     }

    //     return true;
    // }
}




