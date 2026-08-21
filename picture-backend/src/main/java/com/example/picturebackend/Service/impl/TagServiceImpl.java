package com.example.picturebackend.Service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.picturebackend.constant.UserConstant;
import com.example.picturebackend.Mapper.TagMapper;
import com.example.picturebackend.Mapper.PictureTagMapper;
import com.example.picturebackend.Mapper.SpaceMapper;
import com.example.picturebackend.Service.PictureTagService;
import com.example.picturebackend.Service.SpaceService;
import com.example.picturebackend.Service.TagService;
import com.example.picturebackend.constant.SpaceConstant;
import com.example.picturebackend.domain.MyEnums.TagStatus;
import com.example.picturebackend.domain.po.PictureTag;
import com.example.picturebackend.domain.po.Tag;
import com.example.picturebackend.domain.po.Space;
import com.example.picturebackend.domain.po.User;
import com.example.picturebackend.Exception.ErrorCode;
import com.example.picturebackend.Exception.ThrowExceptionUtils;
import com.example.picturebackend.manager.MultiCacheManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 空间标签管理服务。
 *
 * <p>这个 Service 是空间标签规则的唯一实现接缝，Controller 不直接操作 TagMapper，
 * 从而把空间权限、额度、名称判重和状态生命周期集中在一个地方。</p>
 */
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
        implements TagService {

    private static final int MAX_TAG_NAME_LENGTH = 32;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceMapper spaceMapper;

    @Resource
    private PictureTagMapper pictureTagMapper;

    @Resource
    private PictureTagService pictureTagService;

    @Resource
    private MultiCacheManager multiCacheManager;
            
    /**
     * 查询空间内的标签
     */
    @Override
    public List<Tag> listBySpaceId(Long spaceId, boolean includeDisabled, User loginUser) {
        validateSpaceId(spaceId); 
        requireLoginUser(loginUser);
        spaceService.SpaceAuthCheck(spaceId, loginUser);

        QueryWrapper<Tag> queryWrapper = new QueryWrapper<Tag>()
                .eq("spaceId", spaceId)
                .orderByAsc("tagName")
                .orderByAsc("id");

        if (!includeDisabled) {
            queryWrapper.eq("status", TagStatus.ACTIVE.getValue());
        }
        return this.list(queryWrapper);
    }

    /**
     * 创建tag
     * 由于要同时更新空间tag数量和保存tag
     * 因此需要事务回滚
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Tag createTag(Long spaceId, String tagName, User loginUser) {
        // 锁定空间行，保证同一空间并发创建标签时不会同时通过额度检查。
        Space space = requireLockedSpaceOwner(spaceId, loginUser);
        // 去掉前后空格
        String displayName = trimTagName(tagName);
        // 拿到小写的，避免大小写含义一样
        String normalizedName = normalizeTagName(displayName);

        // 校验当前空间中标签数量是否已达上限
        long currentCount = this.count(new QueryWrapper<Tag>().eq("spaceId", spaceId));
        long maxCount = SpaceConstant.getMaxTagCountByLevel(space.getSpaceLevel());
        ThrowExceptionUtils.throwIF(
                currentCount >= maxCount,
                ErrorCode.PARAMS_ERROR,
                "空间标签数量已达到上限，请升级空间后再试");
        
        // 校验是否存在重名标签
        boolean duplicated = this.count(new QueryWrapper<Tag>()
                .eq("spaceId", spaceId)
                .eq("normalizedName", normalizedName)) > 0;
        ThrowExceptionUtils.throwIF(
                duplicated,
                ErrorCode.PARAMS_ERROR,
                "该空间已存在同名标签");
        
        // 封装新Tag
        Tag tag = new Tag();
        tag.setSpaceId(spaceId);
        tag.setTagName(displayName);
        tag.setNormalizedName(normalizedName);
        tag.setStatus(TagStatus.ACTIVE.getValue());
        tag.setCreatedBy(loginUser.getId());
        tag.setCreateTime(new Date());
        tag.setUpdateTime(new Date());
        // 保存Tag
        ThrowExceptionUtils.throwIF(!this.save(tag), ErrorCode.OPERATION_ERROR, "标签创建失败");
        return tag;
    }

    /**
     * 标签重命名
     */
    @Override
    public Boolean renameTag(Long tagId, String tagName, User loginUser) {
        Tag tag = getRequiredTag(tagId);
        requireSpaceOwner(tag.getSpaceId(), loginUser);

        String displayName = trimTagName(tagName);
        String normalizedName = normalizeTagName(displayName);
        // 校验是否存在重名标签
        boolean duplicated = this.count(new QueryWrapper<Tag>()
                .eq("spaceId", tag.getSpaceId())
                .eq("normalizedName", normalizedName)
                .ne("id", tagId)) > 0;
        ThrowExceptionUtils.throwIF(
                duplicated,
                ErrorCode.PARAMS_ERROR,
                "该空间已存在同名标签");

        tag.setTagName(displayName);
        tag.setNormalizedName(normalizedName);
        tag.setUpdateTime(new Date());
        boolean updated = this.updateById(tag);
        if (updated) {
            // 改名会改变所有关联图片的显示名称，必须清理图片分页缓存。
            multiCacheManager.invalidatePicturePageCache();
        }
        return updated;
    }

    /**
     * 删除标签
     * 同理需要同时操作space和tag
     * 因此需要事务回滚
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteTag(Long tagId, User loginUser) {
        Tag tag = getRequiredTag(tagId);
        requireSpaceOwner(tag.getSpaceId(), loginUser);

        pictureTagMapper.delete(new QueryWrapper<PictureTag>()
                .eq("tagId", tagId));
        boolean deleted = this.removeById(tagId);
        ThrowExceptionUtils.throwIF(!deleted, ErrorCode.OPERATION_ERROR, "标签删除失败");
        multiCacheManager.invalidatePicturePageCache();
        return true;
    }

    /**
     * 停用标签
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean disableTag(Long tagId, User adminUser) {
        return updateStatus(tagId, TagStatus.DISABLED, adminUser);
    }

    /**
     * 恢复标签
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean restoreTag(Long tagId, User adminUser) {
        return updateStatus(tagId, TagStatus.ACTIVE, adminUser);
    }

    /**
     * 
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateStatus(Long tagId, TagStatus status, User adminUser) {
        // 校验是否是管理员
        requireAdmin(adminUser);
        ThrowExceptionUtils.throwIF(status == null, ErrorCode.PARAMS_ERROR, "标签状态不能为空");

        Tag tag = getRequiredTag(tagId);
        // 从停用修改到恢复状态
        if (TagStatus.ACTIVE.equals(status) && !TagStatus.ACTIVE.getValue().equals(tag.getStatus())) {
            // 恢复可能让历史关联重新变成有效标签，先检查单图最多 3 个规则。
            pictureTagService.validateTagRestore(tagId);
        }

        // 若目标状态与当前状态一致，跳过
        if (status.getValue().equals(tag.getStatus())) {
            return true;
        }

        // 更新tag状态
        tag.setStatus(status.getValue());
        tag.setUpdateTime(new Date());
        boolean updated = this.updateById(tag);
        if (updated) {
            multiCacheManager.invalidatePicturePageCache();
        }
        return updated;
    }

    /** 去除首尾空格并校验展示名称。 */
    static String trimTagName(String rawName) {
        // 标签名称判空
        ThrowExceptionUtils.throwIF(StrUtil.isBlank(rawName), ErrorCode.PARAMS_ERROR, "标签名称不能为空");
        // 去除前后空格
        String displayName = rawName.trim();
        // 校验标签长度合法性
        ThrowExceptionUtils.throwIF(
                displayName.length() > MAX_TAG_NAME_LENGTH,
                ErrorCode.PARAMS_ERROR,
                "标签名称不能超过 32 个字符");
        return displayName;
    }

    /** 仅用于同空间判重，展示名称本身不强制改成小写。 */
    static String normalizeTagName(String displayName) {
        return displayName.toLowerCase(Locale.ROOT);
    }

    // 校验标签是否存在
    private Tag getRequiredTag(Long tagId) {
        ThrowExceptionUtils.throwIF(tagId == null, ErrorCode.PARAMS_ERROR, "标签 id 不能为空");
        Tag tag = this.getById(tagId);
        ThrowExceptionUtils.throwIF(tag == null, ErrorCode.NOT_FOUND_ERROR, "标签不存在");
        return tag;
    }

    // 校验空间是否存在且是否属于当前登录用户
    private Space requireSpaceOwner(Long spaceId, User loginUser) {
        validateSpaceId(spaceId);
        requireLoginUser(loginUser);
        Space space = spaceService.getById(spaceId);
        ThrowExceptionUtils.throwIF(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        ThrowExceptionUtils.throwIF(
                !space.getUserId().equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR,
                "只有空间持有人可以管理标签");
        return space;
    }

    /**
     * 创建标签专用的持有人校验。
     *
     * <p>这里必须直接通过 Mapper 加 FOR UPDATE，不能只读取后再统计数量，
     * 否则两个并发请求可能都在旧数量上通过检查，突破空间标签额度。</p>
     */
    private Space requireLockedSpaceOwner(Long spaceId, User loginUser) {
        // 判断空间id是否合法
        validateSpaceId(spaceId);
        // 判断是否登录
        requireLoginUser(loginUser);
        // 查询锁空间
        Space space = spaceMapper.selectOne(new QueryWrapper<Space>()
                .eq("id", spaceId)
                .last("FOR UPDATE"));
        // 校验空间
        ThrowExceptionUtils.throwIF(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        // 校验权限
        ThrowExceptionUtils.throwIF(
                !space.getUserId().equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR,
                "只有空间持有人可以管理标签");
        return space;
    }

    /**
     * 校验管理员身份
     * @param loginUser
     */
    private void requireAdmin(User loginUser) {
        requireLoginUser(loginUser);
        ThrowExceptionUtils.throwIF(
                !UserConstant.ADMIN_ROLE.equals(loginUser.getUserLevel()),
                ErrorCode.NO_AUTH_ERROR,
                "只有管理员可以审核标签");
    }

    /**
     * 判断当前用户是否已登录
     * @param loginUser
     */
    private void requireLoginUser(User loginUser) {
        ThrowExceptionUtils.throwIF(loginUser == null || loginUser.getId() == null,
                ErrorCode.NOT_LOGIN_ERROR,
                "请先登录");
    }

    /**
     * 判断目标空间是否是个人空间
     * @param spaceId
     */
    private void validateSpaceId(Long spaceId) {
        ThrowExceptionUtils.throwIF(spaceId == null || spaceId <= 0,
                ErrorCode.PARAMS_ERROR,
                "空间标签必须属于个人空间");
    }

}




