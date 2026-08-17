package com.example.picturebackend.Service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.picturebackend.Exception.ErrorCode;
import com.example.picturebackend.Exception.BusinessException;
import com.example.picturebackend.Exception.ThrowExceptionUtils;
import com.example.picturebackend.Mapper.UserMapper;
import com.example.picturebackend.Mapper.UserNotificationMapper;
import com.example.picturebackend.Mapper.PictureMapper;
import com.example.picturebackend.Service.UserNotificationService;
import com.example.picturebackend.constant.NotificationConstant;
import com.example.picturebackend.domain.po.User;
import com.example.picturebackend.domain.po.Picture;
import com.example.picturebackend.domain.po.UserNotification;
import com.example.picturebackend.domain.request.notification.NotificationCreateRequest;
import com.example.picturebackend.domain.request.notification.NotificationPublishRequest;
import com.example.picturebackend.domain.request.notification.NotificationQueryRequest;
import com.example.picturebackend.domain.vo.notification.NotificationVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户通知业务实现。
 */
@Service
public class UserNotificationServiceImpl
        extends ServiceImpl<UserNotificationMapper, UserNotification>
        implements UserNotificationService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private PictureMapper pictureMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer publishNotification(NotificationPublishRequest request) {
        validatePublishRequest(request);
        List<User> users;
        if (Boolean.TRUE.equals(request.getAllUsers())) {
            // MP 会自动追加 isDelete=0，封禁、管理员和 VIP 均不会被排除。
            users = userMapper.selectList(new QueryWrapper<>());
        } else {
            User user = userMapper.selectById(request.getUserId());
            ThrowExceptionUtils.throwIF(user == null, ErrorCode.NOT_FOUND_ERROR, "指定用户不存在");
            users = List.of(user);
        }

        Long batchId = IdWorker.getId();
        for (User user : users) {
            NotificationCreateRequest createRequest = new NotificationCreateRequest();
            createRequest.setType(request.getType());
            createRequest.setUserId(user.getId());
            createRequest.setBizId(batchId);
            createRequest.setTitle(request.getTitle());
            createRequest.setContent(request.getContent());
            createNotification(createRequest);
        }
        return users.size();
    }

    @Override
    public IPage<NotificationVO> queryNotificationPage(NotificationQueryRequest request, Long currentUserId) {
        ThrowExceptionUtils.throwIF(currentUserId == null || request == null, ErrorCode.PARAMS_ERROR);
        ThrowExceptionUtils.throwIF(request.getCurrent() == null || request.getCurrent() < 1,
                ErrorCode.PARAMS_ERROR, "页码必须大于等于1");
        ThrowExceptionUtils.throwIF(request.getPageSize() == null || request.getPageSize() < 1 || request.getPageSize() > 50,
                ErrorCode.PARAMS_ERROR, "每页条数必须在1到50之间");
        QueryWrapper<UserNotification> wrapper = new QueryWrapper<UserNotification>()
                .eq("userId", currentUserId);
        if (Boolean.TRUE.equals(request.getUnreadOnly())) {
            wrapper.isNull("readTime");
        }
        wrapper.orderByDesc("createTime").orderByDesc("id");
        Page<UserNotification> source = new Page<>(request.getCurrent(), request.getPageSize());
        IPage<UserNotification> page = baseMapper.selectPage(source, wrapper);
        Page<NotificationVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        Map<Long, String> pictureNameMap = queryPictureNameMap(page.getRecords());
        result.setRecords(page.getRecords().stream()
                .map(notification -> toNotificationVO(notification, pictureNameMap))
                .toList());
        return result;
    }

    @Override
    public Integer deleteNotifications(List<Long> ids, Long currentUserId) {
        ThrowExceptionUtils.throwIF(currentUserId == null || ids == null || ids.isEmpty() || ids.size() > 100,
                ErrorCode.PARAMS_ERROR);
        List<Long> distinctIds = ids.stream().distinct().toList();
        ThrowExceptionUtils.throwIF(distinctIds.stream().anyMatch(Objects::isNull) || distinctIds.stream().anyMatch(id -> id <= 0),
                ErrorCode.PARAMS_ERROR);
        return baseMapper.delete(new UpdateWrapper<UserNotification>()
                .eq("userId", currentUserId)
                .in("id", distinctIds));
    }

    @Override
    public Integer markNotificationsRead(List<Long> ids, Long currentUserId) {
        ThrowExceptionUtils.throwIF(currentUserId == null || ids == null || ids.isEmpty() || ids.size() > 100,
                ErrorCode.PARAMS_ERROR);
        List<Long> distinctIds = ids.stream().distinct().toList();
        ThrowExceptionUtils.throwIF(distinctIds.stream().anyMatch(Objects::isNull) || distinctIds.stream().anyMatch(id -> id <= 0),
                ErrorCode.PARAMS_ERROR);
        return baseMapper.update(null, new UpdateWrapper<UserNotification>()
                .eq("userId", currentUserId)
                .isNull("readTime")
                .in("id", distinctIds)
                .set("readTime", new Date()));
    }

    @Override
    public Boolean createNotification(NotificationCreateRequest request) {
        ThrowExceptionUtils.throwIF(request == null || StrUtil.isBlank(request.getType())
                        || request.getUserId() == null || request.getBizId() == null
                        || StrUtil.isBlank(request.getTitle()) || StrUtil.isBlank(request.getContent()),
                ErrorCode.PARAMS_ERROR, "通知参数不能为空");
        String bizType = switch (request.getType()) {
            case NotificationConstant.TYPE_SYSTEM_ANNOUNCEMENT ->
                    NotificationConstant.BIZ_TYPE_SYSTEM_ANNOUNCEMENT;
            case NotificationConstant.TYPE_AVATAR_REVIEW_RESULT ->
                    NotificationConstant.BIZ_TYPE_AVATAR_CHECK;
            case NotificationConstant.TYPE_PICTURE_REVIEW_RESULT ->
                    NotificationConstant.BIZ_TYPE_PICTURE_CHECK;
            default -> throw new com.example.picturebackend.Exception.BusinessException(
                    ErrorCode.PARAMS_ERROR, "未知通知类型");
        };
        UserNotification notification = new UserNotification();
        notification.setId(IdWorker.getId());
        notification.setUserId(request.getUserId());
        notification.setType(request.getType());
        notification.setBizType(bizType);
        notification.setBizId(request.getBizId());
        notification.setTitle(request.getTitle());
        notification.setContent(request.getContent());
        notification.setReadTime(null);
        notification.setIsDelete(0);
        notification.setCreateTime(new Date());
        notification.setUpdateTime(notification.getCreateTime());
        if (NotificationConstant.TYPE_PICTURE_REVIEW_RESULT.equals(notification.getType())) {
            UserNotification existing = baseMapper.selectOneIncludingDeleted(
                    notification.getUserId(), notification.getType(), notification.getBizType(), notification.getBizId());
            if (existing != null) {
                updatePictureNotification(existing, notification);
                return true;
            }
        }
        try {
            ThrowExceptionUtils.throwIF(baseMapper.insert(notification) <= 0,
                    ErrorCode.OPERATION_ERROR, "通知保存失败");
            return true;
        } catch (DuplicateKeyException exception) {
            if (NotificationConstant.TYPE_PICTURE_REVIEW_RESULT.equals(notification.getType())) {
                UserNotification existing = baseMapper.selectOneIncludingDeleted(
                        notification.getUserId(), notification.getType(), notification.getBizType(), notification.getBizId());
                if (existing != null) {
                    updatePictureNotification(existing, notification);
                    return true;
                }
            }
            UserNotification existing = baseMapper.selectOne(new QueryWrapper<UserNotification>()
                    .eq("userId", notification.getUserId())
                    .eq("type", notification.getType())
                    .eq("bizType", notification.getBizType())
                    .eq("bizId", notification.getBizId()));
            if (existing != null) {
                return true;
            }
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "通知保存失败");
        }
    }

    private void updatePictureNotification(UserNotification existing, UserNotification latest) {
        existing.setUserId(latest.getUserId());
        existing.setType(latest.getType());
        existing.setTitle(latest.getTitle());
        existing.setContent(latest.getContent());
        existing.setBizType(latest.getBizType());
        existing.setBizId(latest.getBizId());
        existing.setReadTime(null);
        existing.setIsDelete(0);
        // 复审代表最新通知，刷新两个时间字段以保持分页排序正确。
        existing.setCreateTime(latest.getCreateTime());
        existing.setUpdateTime(latest.getUpdateTime());
        ThrowExceptionUtils.throwIF(baseMapper.updateIncludingDeleted(existing) <= 0,
                ErrorCode.OPERATION_ERROR, "通知更新失败");
    }

    private void validatePublishRequest(NotificationPublishRequest request) {
        ThrowExceptionUtils.throwIF(request == null || request.getAllUsers() == null,
                ErrorCode.PARAMS_ERROR);
        ThrowExceptionUtils.throwIF(!NotificationConstant.TYPE_SYSTEM_ANNOUNCEMENT.equals(request.getType()),
                ErrorCode.PARAMS_ERROR, "管理员只能发布系统公告");
        boolean allUsers = Boolean.TRUE.equals(request.getAllUsers());
        ThrowExceptionUtils.throwIF(allUsers ? request.getUserId() != null : request.getUserId() == null || request.getUserId() <= 0,
                ErrorCode.PARAMS_ERROR, "通知目标参数不合法");
        ThrowExceptionUtils.throwIF(StrUtil.isBlank(request.getTitle()) || request.getTitle().length() > 128
                        || StrUtil.isBlank(request.getContent()) || request.getContent().length() > 1024,
                ErrorCode.PARAMS_ERROR, "通知标题或内容不合法");
    }

    private Map<Long, String> queryPictureNameMap(List<UserNotification> notifications) {
        Set<Long> pictureIds = notifications.stream()
                .filter(notification -> NotificationConstant.BIZ_TYPE_PICTURE_CHECK.equals(notification.getBizType()))
                .map(UserNotification::getBizId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (pictureIds.isEmpty()) {
            return Map.of();
        }
        return pictureMapper.selectBatchIds(pictureIds).stream()
                .filter(picture -> picture.getId() != null && picture.getName() != null)
                .collect(Collectors.toMap(Picture::getId, Picture::getName, (left, right) -> left));
    }

    private NotificationVO toNotificationVO(UserNotification notification, Map<Long, String> pictureNameMap) {
        NotificationVO vo = new NotificationVO();
        vo.setId(notification.getId());
        vo.setType(notification.getType());
        vo.setTitle(notification.getTitle());
        vo.setContent(notification.getContent());
        vo.setBizType(notification.getBizType());
        vo.setBizId(notification.getBizId());
        vo.setBizName(notification.getBizId() == null ? null : pictureNameMap.get(notification.getBizId()));
        vo.setReadTime(notification.getReadTime());
        vo.setCreateTime(notification.getCreateTime());
        return vo;
    }
}
