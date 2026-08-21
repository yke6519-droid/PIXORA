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

    /** 空间标签额度不复用图片数量上限，单独按空间等级计算。 */
    public static final Long NORMAL_MAX_TAG_COUNT = 10L;
    public static final Long Professional_MAX_TAG_COUNT = 20L;
    public static final Long Expert_MAX_TAG_COUNT = 30L;

    public static  SpaceLevel getSizeAndCountByLevel(Integer level){
        validateLevel(level);

        SpaceLevel spaceLevel = new SpaceLevel();
        if(NORMAL_LEVEL.equals(level)){
            spaceLevel.setLevel(NORMAL_LEVEL);
            spaceLevel.setMaxSize(NORMAL_MAX_SIZE);
            spaceLevel.setMaxCount(NORMAL_MAX_Count);
            return spaceLevel;
        }else if(Professional_LEVEL.equals(level)){
            spaceLevel.setLevel(Professional_LEVEL);
            spaceLevel.setMaxSize(Professional_MAX_SIZE);
            spaceLevel.setMaxCount(Professional_MAX_Count);
            return spaceLevel;
        }else if(Expert_LEVEL.equals(level)){
            spaceLevel.setLevel(Expert_LEVEL);
            spaceLevel.setMaxSize(Expert_MAX_SIZE);
            spaceLevel.setMaxCount(Expert_MAX_Count);
            return spaceLevel;
        }

        return spaceLevel;
    }

    /**
     * 根据空间等级返回空间标签定义数量上限。
     * ACTIVE、DISABLED 两种状态都会占用额度，只有删除标签实体后才释放。
     */
    public static Long getMaxTagCountByLevel(Integer level) {
        validateLevel(level);
        if (NORMAL_LEVEL.equals(level)) {
            return NORMAL_MAX_TAG_COUNT;
        } else if (Professional_LEVEL.equals(level)) {
            return Professional_MAX_TAG_COUNT;
        }
        return Expert_MAX_TAG_COUNT;
    }

    /** 统一校验空间等级，避免图片容量和标签额度使用两套不一致的规则。 */
    private static void validateLevel(Integer level) {
        ThrowExceptionUtils.throwIF(
                ObjectUtil.isNull(level),
                ErrorCode.PARAMS_ERROR,
                "空间等级为空");
        ThrowExceptionUtils.throwIF(
                !NORMAL_LEVEL.equals(level)
                        && !Professional_LEVEL.equals(level)
                        && !Expert_LEVEL.equals(level),
                ErrorCode.PARAMS_ERROR,
                "空间等级不合法");
    }
}
