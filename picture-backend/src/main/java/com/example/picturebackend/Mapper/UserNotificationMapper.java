package com.example.picturebackend.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.picturebackend.domain.po.UserNotification;
import org.apache.ibatis.annotations.Param;

/**
 * 用户通知表 Mapper。
 */
public interface UserNotificationMapper extends BaseMapper<UserNotification> {
    UserNotification selectOneIncludingDeleted(@Param("userId") Long userId,
                                                @Param("type") String type,
                                                @Param("bizType") String bizType,
                                                @Param("bizId") Long bizId);

    int updateIncludingDeleted(UserNotification notification);
}
