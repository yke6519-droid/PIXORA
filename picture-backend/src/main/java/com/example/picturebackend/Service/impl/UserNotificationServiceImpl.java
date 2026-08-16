package com.example.picturebackend.Service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.picturebackend.Mapper.UserNotificationMapper;
import com.example.picturebackend.Service.UserNotificationService;
import com.example.picturebackend.domain.po.UserNotification;
import org.springframework.stereotype.Service;

/**
 * 用户通知业务实现。
 */
@Service
public class UserNotificationServiceImpl
        extends ServiceImpl<UserNotificationMapper, UserNotification>
        implements UserNotificationService {
}
