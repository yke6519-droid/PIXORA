package com.example.picturebackend.aop;

import com.example.picturebackend.Exception.ErrorCode;
import com.example.picturebackend.Exception.ThrowExceptionUtils;
import com.example.picturebackend.Service.UserService;
import com.example.picturebackend.annotation.AuthCheck;
import com.example.picturebackend.constant.UserConstant;
import com.example.picturebackend.domain.MyEnums.UserLevel;
import com.example.picturebackend.domain.po.User;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

import cn.hutool.core.util.StrUtil;

@Aspect
@Component
public class AuthInterceptor {
    @Resource
    private UserService userService;

    /**
     * 执行拦截
     * @param joinPoint
     * @param authCheck
     * @return
     * @throws Throwable
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        // 拿到当前的request
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 获取当前登录用户
        User currentUser = userService.getCurrentUser(request);
        // mustRole 为空表示不限制角色，直接放行
        if (StrUtil.isBlank(mustRole)) {
            return joinPoint.proceed();
        }
        UserLevel mustRoleEnum = UserLevel.getEnumByValue(mustRole);
        // 如果没有设权限 或 运行本人操作 则放行
        if (mustRoleEnum == null){
            return joinPoint.proceed();
        }
        // 必须有权限才能通过下面的代码
        UserLevel currentUserEnum = UserLevel.getEnumByValue(currentUser.getUserLevel());
        ThrowExceptionUtils.throwIF(
                // 用户角色为空，或不等于当前必须的角色
                currentUserEnum == null,
                // 抛出无权限异常
                ErrorCode.NO_AUTH_ERROR
        );
        System.out.println("必须要有的权限为："+mustRoleEnum.getValue());
        System.out.println("当前用户权限为："+currentUserEnum.getValue());
        //1. 管理员权限校验
        ThrowExceptionUtils.throwIF(
                // 需要管理员权限
                mustRoleEnum.getValue().equals(UserConstant.ADMIN_ROLE)
                // 且当前用户没有管理员权限
                        && !currentUserEnum.getValue().equals(UserConstant.ADMIN_ROLE),
                ErrorCode.NO_AUTH_ERROR,
                "当前操作需要管理员权限"
        );
        //2. 权限满足校验
        ThrowExceptionUtils.throwIF(
                // 需要管理员权限
                ! mustRoleEnum.getValue().equals(currentUserEnum.getValue()),
                ErrorCode.NO_AUTH_ERROR
        );
        // 若都不满足 则放行
        return joinPoint.proceed();
    }
}
