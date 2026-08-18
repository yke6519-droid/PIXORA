package com.example.picturebackend.constant;

import com.example.picturebackend.Exception.ErrorCode;
import com.example.picturebackend.Exception.ThrowExceptionUtils;
import com.example.picturebackend.domain.vo.space.SpaceLevel;

import cn.hutool.core.util.ObjectUtil;
import lombok.Data;

@Data
public class SpaceConstant {
    public static final Integer NORMAL_LEVEL = 0;
    public static final Long NORMAL_MAX_SIZE = (long) (100*1024*1024);
    public static final Long NORMAL_MAX_Count = 50L;
    public static final Integer Professional_LEVEL = 1;
    public static final Long Professional_MAX_SIZE = (long) (500 * 1024 *1024);
    public static final Long Professional_MAX_Count = 100L;
    public static final Integer Expert_LEVEL = 2;
    public static final Long Expert_MAX_SIZE = (long) (1000 * 1024 *1024);
    public static final Long Expert_MAX_Count = 200L;

    public static  SpaceLevel getSizeAndCountByLevel(Integer level){
        // 参数判空校验
        ThrowExceptionUtils.throwIF(
            ObjectUtil.isNull(level), 
            ErrorCode.PARAMS_ERROR,
            "空间等级为空");
        // 空间等级合法性校验
        ThrowExceptionUtils.throwIF(
            level != NORMAL_LEVEL && level != Professional_LEVEL && level != Expert_LEVEL, 
            ErrorCode.PARAMS_ERROR,
            "空间等级不合法");

        SpaceLevel spaceLevel = new SpaceLevel();
        if(level == NORMAL_LEVEL){
            spaceLevel.setLevel(NORMAL_LEVEL);
            spaceLevel.setMaxSize(NORMAL_MAX_SIZE);
            spaceLevel.setMaxCount(NORMAL_MAX_Count);
            return spaceLevel;
        }else if(level == Professional_LEVEL){
            spaceLevel.setLevel(Professional_LEVEL);
            spaceLevel.setMaxSize(Professional_MAX_SIZE);
            spaceLevel.setMaxCount(Professional_MAX_Count);
            return spaceLevel;
        }else if(level == Expert_LEVEL){
            spaceLevel.setLevel(Expert_LEVEL);
            spaceLevel.setMaxSize(Expert_MAX_SIZE);
            spaceLevel.setMaxCount(Expert_MAX_Count);
            return spaceLevel;
        }

        return spaceLevel;
    }
}
