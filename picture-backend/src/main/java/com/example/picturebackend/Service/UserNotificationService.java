package com.example.picturebackend.Service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.picturebackend.domain.po.UserNotification;
import com.example.picturebackend.domain.request.notification.NotificationCreateRequest;
import com.example.picturebackend.domain.request.notification.NotificationPublishRequest;
import com.example.picturebackend.domain.request.notification.NotificationQueryRequest;
import com.example.picturebackend.domain.request.notification.NotificationReadRequest;
import com.example.picturebackend.domain.vo.notification.NotificationVO;

import java.util.List;

/**
 * 用户通知业务接口。
 *
 * <p>提供系统通知发布、通用类型驱动通知创建、当前用户查询和删除能力。</p>
 */
public interface UserNotificationService extends IService<UserNotification> {
    Integer publishNotification(NotificationPublishRequest request);

    IPage<NotificationVO> queryNotificationPage(NotificationQueryRequest request, Long currentUserId);

    Integer deleteNotifications(List<Long> ids, Long currentUserId);

    Integer markNotificationsRead(List<Long> ids, Long currentUserId);

    Boolean createNotification(NotificationCreateRequest request);
}
