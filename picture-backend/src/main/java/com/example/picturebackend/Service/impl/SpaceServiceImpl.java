package com.example.picturebackend.Service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.picturebackend.Exception.ErrorCode;
import com.example.picturebackend.Exception.ThrowExceptionUtils;
import com.example.picturebackend.Mapper.PictureMapper;
import com.example.picturebackend.Mapper.SpaceMapper;
import com.example.picturebackend.Service.PictureService;
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
import com.example.picturebackend.domain.vo.SpacePageVO;
import com.example.picturebackend.domain.vo.SpaceVO;
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
     * todo: 前端进入当我的空间界面后，
     * todo：如果当前用户已经有空间，则直接查询当前空间中的图片，返回给前端展示
     * todo：若没有空间，则展示当前用户并没有创建空间，并提供一个按钮引导用户创建空间
     */

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
        ThrowExceptionUtils.throwIF(!Objects.equals(loginUser.getId(), space.getUserId())
                        && !loginUser.getUserstatus().equals(UserConstant.ADMIN_ROLE),
                ErrorCode.NO_AUTH_ERROR);
        //1. 先删除该空间中的图片
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<Picture>().eq("spaceId",spaceId);
        pictureMapper.delete(queryWrapper);
        //2. 再删除该空间
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
        ThrowExceptionUtils.throwIF(ObjectUtil.isNull(spaceId) || StrUtil.isBlank(updatedName),
                ErrorCode.PARAMS_ERROR);
        Space space = this.getById(spaceId);
        ThrowExceptionUtils.throwIF(ObjectUtil.isNull(space),ErrorCode.PARAMS_ERROR,"目标空间不存在");
        ThrowExceptionUtils.throwIF(!Objects.equals(loginUser.getId(), space.getUserId())
                        || !loginUser.getUserstatus().equals(UserConstant.ADMIN_ROLE),
                ErrorCode.NO_AUTH_ERROR);
        space.setSpaceName(updatedName);
        space.setUpdateTime(DateTime.now());
        return this.updateById(space);
    }

    @Override
    public boolean alterLevelById(AlterLevelRequest alterLevelRequest, User loginUser) {
        Long spaceId = alterLevelRequest.getSpaceId();
        Integer alterLevel = alterLevelRequest.getAlterLevel();
        ThrowExceptionUtils.throwIF(ObjectUtil.isNull(spaceId) || ObjectUtil.isNull(alterLevel)
                ,ErrorCode.PARAMS_ERROR);
        ThrowExceptionUtils.throwIF(alterLevel<0 || alterLevel>2,ErrorCode.PARAMS_ERROR,"等级设置非法");
        Space space = this.getById(spaceId);
        ThrowExceptionUtils.throwIF(ObjectUtil.isNull(space),ErrorCode.PARAMS_ERROR,"目标空间不存在");
        space.setSpaceLevel(alterLevel);
        return this.updateById(space);
    }

    @Override
    public SpaceVO querySpace(Long spaceId, User loginUser) {
        Space space = this.getById(spaceId);
        ThrowExceptionUtils.throwIF(ObjectUtil.isNull(space),
                ErrorCode.PARAMS_ERROR,"目标空间不存在");
        ThrowExceptionUtils.throwIF(!space.getUserId().equals(loginUser.getId()) &&
                !loginUser.getUserstatus().equals(UserConstant.ADMIN_ROLE),
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
}




