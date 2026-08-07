package com.example.picturebackend.Service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.picturebackend.Exception.ErrorCode;
import com.example.picturebackend.Exception.ThrowExceptionUtils;
import com.example.picturebackend.Mapper.PictureMapper;
import com.example.picturebackend.Mapper.SpaceMapper;

import com.example.picturebackend.Service.SpaceService;
import com.example.picturebackend.Service.UserService;
import com.example.picturebackend.constant.SpaceConstant;
import com.example.picturebackend.constant.UserConstant;
import com.example.picturebackend.domain.po.Picture;
import com.example.picturebackend.domain.po.Space;
import com.example.picturebackend.domain.po.User;
import com.example.picturebackend.domain.request.space.AlterLevelRequest;
import com.example.picturebackend.domain.request.space.CreateSpaceRequest;
import com.example.picturebackend.domain.request.space.SpaceQueryRequest;
import com.example.picturebackend.domain.request.space.SpaceUpdateRequest;
import com.example.picturebackend.domain.vo.space.SpaceLevel;
import com.example.picturebackend.domain.vo.space.SpacePageVO;
import com.example.picturebackend.domain.vo.space.SpaceVO;

import org.joda.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.Objects;
import java.util.stream.Collectors;

/**
* @author chen
* @description 针对表【space(用户私有空间表)】的数据库操作Service实现
* @createDate 2026-06-02 15:45:36
*/
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService {
    @Resource
    private PictureMapper pictureMapper;
    @Resource
    private UserService userService;

    /**
     * 创建空间
     * @param createSpaceRequest
     * @param loginUser
     * @return
     */
    @Override
    @Transactional
    public Space createSpace(CreateSpaceRequest createSpaceRequest, User loginUser){
        // 判断DB中是否已经有改用户的私人空间
        boolean exists = this.query().eq("userId", loginUser.getId()).exists();
        ThrowExceptionUtils.throwIF(exists, ErrorCode.SYSTEM_ERROR, "当前用户已拥有私人空间");
        String spaceName = createSpaceRequest.getSpaceName();
        ThrowExceptionUtils.throwIF(StrUtil.isBlank(spaceName), ErrorCode.PARAMS_ERROR,"空间名为空");
        // 填充space
        Space space = new Space();
        space.setSpaceName(spaceName);
        space.setSpaceLevel(0);
        space.setMaxSize(SpaceConstant.NORMAL_MAX_SIZE);
        space.setMaxCount(SpaceConstant.NORMAL_MAX_Count);
        space.setUserId(loginUser.getId());
        space.setCreateTime(DateTime.now());
        space.setUpdateTime(DateTime.now());
        // 存入DB
        this.save(space);
        // 同步用户的spaceId
        loginUser.setSpaceId(space.getId());
        userService.updateById(loginUser);
        return space;
    }

    // todo 后续可以考虑根据用户等级，来决定是否允许创建多个私人空间
    // @Override
    // @Transactional
    // public Space createSpaceByUserLevel(CreateSpaceRequest createSpaceRequest, User loginUser){

    //     // 判断DB中是否已经有改用户的私人空间
    //     // 1. 校验登录用户的UserLevel
    //     final String userLevel = loginUser.getUserLevel();

    //     // 1.1 若为普通用户，则只能创建一个私人空间
    //     if (userLevel.equals(UserConstant.DEFAULT_ROLE)) {

    //         // 若超过限制，则抛出异常，提示用户无法创建更多
    //         if (this.query().eq("userId", loginUser.getId()).exists()) {
    //             ThrowExceptionUtils.throwIF(true, ErrorCode.SYSTEM_ERROR, "当前用户已拥有私人空间");
    //         }
    //     }

    //     // 1.2 若为vip或是管理员，则可以创建三个私人空间
    //     else if (userLevel.equals(UserConstant.VIP_ROLE) || userLevel.equals(UserConstant.ADMIN_ROLE)) {

    //         final long vipSpaceCount = this.query().eq("userId", loginUser.getId()).count();
    //         // 若超过限制，则抛出异常，提示用户无法创建更多
    //         if (vipSpaceCount >= 3) {
    //             ThrowExceptionUtils.throwIF(true, ErrorCode.SYSTEM_ERROR, "当前用户已拥有三个私人空间，无法创建更多");
    //         }
    //     }

    //     // 参数校验
    //     String spaceName = createSpaceRequest.getSpaceName();
    //     ThrowExceptionUtils.throwIF(StrUtil.isBlank(spaceName), ErrorCode.PARAMS_ERROR,"空间名为空");

    //     // 填充space
    //     // todo 后续可以考虑区分vip和普通用户的空间等级，来决定默认空间的最大容量和最大图片数量

    //     Space space = new Space();
    //     space.setSpaceName(spaceName);
    //     space.setSpaceLevel(0);
    //     space.setMaxSize(SpaceConstant.NORMAL_MAX_SIZE);
    //     space.setMaxCount(SpaceConstant.NORMAL_MAX_Count);
    //     space.setUserId(loginUser.getId());
    //     space.setCreateTime(DateTime.now());
    //     space.setUpdateTime(DateTime.now());

    //     // 存入DB
    //     this.save(space);
    //     // 同步用户的spaceId
    //     loginUser.setSpaceId(space.getId());
    //     userService.updateById(loginUser);
    //     return space;
    // }

    /**
     * 根据id删除空间
     * 加上事物回归
     * @param spaceId
     * @param loginUser
     * @return
     */
    @Override
    @Transactional
    public Boolean deleteById(Long spaceId, User loginUser) {
        Space space = this.getById(spaceId);
        ThrowExceptionUtils.throwIF(ObjectUtil.isNull(space),ErrorCode.PARAMS_ERROR,"目标空间不存在");
        ThrowExceptionUtils.throwIF(
                !Objects.equals(loginUser.getId(), space.getUserId())
                        && !loginUser.getUserLevel().equals(UserConstant.ADMIN_ROLE),
                ErrorCode.NO_AUTH_ERROR
        );

        //1. 先删除该空间中的图片
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<Picture>().eq("spaceId",spaceId);
        pictureMapper.delete(queryWrapper);

        //2. 显式 SET NULL，避免 MyBatis-Plus 默认的非空字段更新策略跳过 spaceId。
        boolean userUpdated = userService.update(
                Wrappers.<User>update()
                        .eq("id", space.getUserId())
                        .set("spaceId", null)
        );
        ThrowExceptionUtils.throwIF(!userUpdated, ErrorCode.OPERATION_ERROR, "用户空间状态更新失败");

        //3. 再删除该空间
        return this.removeById(spaceId);
    }

    /**
     * 更新空间
     * @param spaceUpdateRequest
     * @param loginUser
     * @return
     */
    @Override
    public Boolean updateById(SpaceUpdateRequest spaceUpdateRequest, User loginUser) {
        Long spaceId = spaceUpdateRequest.getSpaceId();

        String updatedName = spaceUpdateRequest.getUpdatedName();

        // 空间id判空
        ThrowExceptionUtils.throwIF(ObjectUtil.isNull(spaceId) || StrUtil.isBlank(updatedName),
                ErrorCode.PARAMS_ERROR);

        Space space = this.getById(spaceId);
        ThrowExceptionUtils.throwIF(ObjectUtil.isNull(space),ErrorCode.PARAMS_ERROR,"目标空间不存在");
        
        ThrowExceptionUtils.throwIF(
                !Objects.equals(loginUser.getId(), space.getUserId())
                        && !loginUser.getUserLevel().equals(UserConstant.ADMIN_ROLE),
                ErrorCode.NO_AUTH_ERROR
        );

        space.setSpaceName(updatedName);

        space.setUpdateTime(DateTime.now());

        return this.updateById(space);
    }

    /**
     * 修改空间等级
     * @param alterLevelRequest
     * @param loginUser
     * @return
     */
    @Override
    public boolean alterLevelById(AlterLevelRequest alterLevelRequest, User loginUser) {
        // 拿到请求体中的参数
        final Long spaceId = alterLevelRequest.getSpaceId();
        final Integer alterLevel = alterLevelRequest.getAlterLevel();
        // 参数判空校验
        ThrowExceptionUtils.throwIF(ObjectUtil.isNull(spaceId) || ObjectUtil.isNull(alterLevel)
                ,ErrorCode.PARAMS_ERROR);
        // 判断待等级是否合法
        ThrowExceptionUtils.throwIF(alterLevel<0 || alterLevel>2,ErrorCode.PARAMS_ERROR,"等级设置非法");
        // 拿到对应待修改space对象
        Space space = this.getById(spaceId);
        // 判断待修改空间是否存在
        ThrowExceptionUtils.throwIF(ObjectUtil.isNull(space),ErrorCode.PARAMS_ERROR,"目标空间不存在");
        // 更新space等级、更新时间 与 最大容量
        SpaceLevel spaceLevel = SpaceConstant.getSizeAndCountByLevel(alterLevel);
        
        space.setSpaceLevel(alterLevel);
        space.setUpdateTime(DateTime.now());
        space.setMaxSize(spaceLevel.getMaxSize());
        space.setMaxCount(spaceLevel.getMaxCount());

        boolean result = this.updateById(space);

        return result;
    }

    @Override
    public SpaceVO querySpace(Long spaceId, User loginUser) {
        Space space = this.getById(spaceId);
        ThrowExceptionUtils.throwIF(ObjectUtil.isNull(space),
                ErrorCode.PARAMS_ERROR,"目标空间不存在");
        ThrowExceptionUtils.throwIF(!space.getUserId().equals(loginUser.getId()) &&
                !loginUser.getUserLevel().equals(UserConstant.ADMIN_ROLE),
                ErrorCode.NO_AUTH_ERROR);
        SpaceVO spaceVO = new SpaceVO();
        spaceVO = spaceVO.Space2SpaceVO(space);
        spaceVO.setCreatedUser(userService.getSaftyUser(loginUser));
        return spaceVO;
    }

    @Override
    public SpacePageVO querySpacePage(SpaceQueryRequest spaceQueryRequest) {
        // 构造查询条件
        QueryWrapper<Space> spaceQueryWrapper = this.getQueryWrapper(spaceQueryRequest);
        Integer current = spaceQueryRequest.getCurrent();
        Integer pageSize = spaceQueryRequest.getPageSize();
        Page<Space> spacePage = new Page<>(current,pageSize);
        // 获取分页查询对象
        IPage<Space> spaceIPage = this.page(spacePage,spaceQueryWrapper);
        SpacePageVO spacePageVO = new SpacePageVO();
        // 封装SpaceVO
        spacePageVO.setSpaceVOList(spaceIPage.getRecords().stream().map(this::Space2SPaceVO).collect(Collectors.toList()));
        spacePageVO.setTotal(spaceIPage.getTotal());
        return spacePageVO;
    }

    /**
     * space 转 VO方法
     * @param space
     * @return
     */
    @Override
    public SpaceVO Space2SPaceVO(Space space){
        SpaceVO spaceVO = new SpaceVO();
        spaceVO = spaceVO.Space2SpaceVO(space);
        User user = userService.getById(space.getUserId());
        spaceVO.setCreatedUser(userService.getSaftyUser(user));
        return spaceVO;
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        ThrowExceptionUtils.throwIF(ObjectUtil.isNull(spaceQueryRequest), ErrorCode.PARAMS_ERROR);
        Long id = spaceQueryRequest.getId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortFiled = spaceQueryRequest.getSortFiled();
        String sortOrder = spaceQueryRequest.getSortOrder();

        QueryWrapper<Space> spaceQueryWrapper = new QueryWrapper<>();
        spaceQueryWrapper.eq(!ObjectUtil.isNull(id), "id", id);
        spaceQueryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        spaceQueryWrapper.eq(!ObjectUtil.isNull(spaceLevel), "spaceLevel", spaceLevel);
        spaceQueryWrapper.orderBy(StrUtil.isNotEmpty(sortFiled), sortOrder.equals("ascend"), sortFiled);
        return spaceQueryWrapper;
    }

    @Override
    public void checkUsage(Long spaceId, Picture picture){

        // 校验参数
        ThrowExceptionUtils.throwIF(ObjectUtil.isNull(spaceId),
            ErrorCode.PARAMS_ERROR, "spaceId is null");

        ThrowExceptionUtils.throwIF(ObjectUtil.isNull(picture),
            ErrorCode.PARAMS_ERROR,"picture is null");

        Space space = this.getById(spaceId);

        Long usedCount = space.getUsedCount();
        Long maxCount = space.getMaxCount();
        ThrowExceptionUtils.throwIF(usedCount >= maxCount,
            ErrorCode.OPERATION_ERROR ,"空间图片张数已达上限");

        Long usedSize = space.getUsedSize();
        Long maxSize = space.getMaxSize();
        ThrowExceptionUtils.throwIF(
            usedSize >= maxSize || usedSize+picture.getPicsize() >= maxSize,
            ErrorCode.OPERATION_ERROR ,"空间容量已达上限");
    }

    /**
     * 校验该用户是否有权限操作该空间
     * @param spaceId
     * @param loginUser
     */
    @Override
    public void SpaceAuthCheck(Long spaceId, User loginUser) {
        Space space = this.getById(spaceId);

        ThrowExceptionUtils.throwIF(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");

        ThrowExceptionUtils.throwIF(!space.getUserId().equals(loginUser.getId()),
            ErrorCode.NO_AUTH_ERROR);
    }
}




