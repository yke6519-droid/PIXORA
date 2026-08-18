package com.example.picturebackend.Controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.picturebackend.Service.UserNotificationService;
import com.example.picturebackend.Service.UserService;
import com.example.picturebackend.Utils.ResponseUtils;
import com.example.picturebackend.annotation.AuthCheck;
import com.example.picturebackend.constant.UserConstant;
import com.example.picturebackend.domain.request.BaseResponse;
import com.example.picturebackend.domain.request.notification.NotificationDeleteRequest;
import com.example.picturebackend.domain.request.notification.NotificationPublishRequest;
import com.example.picturebackend.domain.request.notification.NotificationQueryRequest;
import com.example.picturebackend.domain.request.notification.NotificationReadRequest;
import com.example.picturebackend.domain.vo.notification.NotificationVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户通知接口入口。
 *
 * <p>提供管理员发布通知和登录用户查询、删除通知的 HTTP 接口。</p>
 */
@RestController
@RequestMapping("/notification")
public class NotificationController {
    @Resource
    private UserNotificationService userNotificationService;

    @Resource
    private UserService userService;

    @PostMapping("/publish")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> publish(@RequestBody NotificationPublishRequest request) {
        return ResponseUtils.success(userNotificationService.publishNotification(request));
    }

    @PostMapping("/queryPage")
    @AuthCheck
    public BaseResponse<IPage<NotificationVO>> queryPage(@RequestBody NotificationQueryRequest request,
                                                          HttpServletRequest httpRequest) {
        return ResponseUtils.success(userNotificationService.queryNotificationPage(
                request, userService.getCurrentUser(httpRequest).getId()));
    }

    @DeleteMapping("/deleteByIds")
    @AuthCheck
    public BaseResponse<Integer> deleteByIds(@RequestBody NotificationDeleteRequest request,
                                              HttpServletRequest httpRequest) {
        return ResponseUtils.success(userNotificationService.deleteNotifications(
                request == null ? null : request.getIds(), userService.getCurrentUser(httpRequest).getId()));
    }

    @PutMapping("/readByIds")
    @AuthCheck
    public BaseResponse<Integer> readByIds(@RequestBody NotificationReadRequest request,
                                            HttpServletRequest httpRequest) {
        return ResponseUtils.success(userNotificationService.markNotificationsRead(
                request == null ? null : request.getIds(), userService.getCurrentUser(httpRequest).getId()));
    }
}
