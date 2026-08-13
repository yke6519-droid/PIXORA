package com.example.picturebackend.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.service.IService;
import com.example.picturebackend.domain.dto.file.UploadPictureResult;
import com.example.picturebackend.domain.po.Picture;
import com.example.picturebackend.domain.po.Space;
import com.example.picturebackend.domain.po.User;
import com.example.picturebackend.domain.request.space.AlterLevelRequest;
import com.example.picturebackend.domain.request.space.CreateSpaceRequest;
import com.example.picturebackend.domain.request.space.SpaceQueryRequest;
import com.example.picturebackend.domain.request.space.SpaceUpdateRequest;
import com.example.picturebackend.domain.vo.space.SpacePageVO;
import com.example.picturebackend.domain.vo.space.SpaceVO;

/**
* @author chen
* @description 针对表【space(用户私有空间表)】的数据库操作Service
* @createDate 2026-06-02 15:45:36
*/
public interface SpaceService extends IService<Space> {

    /**
     * 创建空间
     * @param createSpaceRequest
     * @param loginUser
     * @return
     */
    Space createSpace(CreateSpaceRequest createSpaceRequest, User loginUser);

    /**
     * 删除空间
     * @param spaceId
     * @param loginUser
     * @return
     */
    Boolean deleteById(Long spaceId, User loginUser);

    /**
     * 用户更新空间
     * @param spaceUpdateRequest
     * @param loginUser
     * @return
     */
    Boolean updateById(SpaceUpdateRequest spaceUpdateRequest, User loginUser);

    /**
     * 更新空间等级
     * @param alterLevelRequest
     * @param loginUser
     * @return
     */
    boolean alterLevelById(AlterLevelRequest alterLevelRequest, User loginUser);

    /**
     * 返回空间信息 + 脱敏后的User信息
     * @param spaceId
     * @param loginUser
     * @return
     */
    SpaceVO querySpace(Long spaceId, User loginUser);

    /**
     * PO 2 VO转换
     * @param space
     * @return
     */
    SpaceVO Space2SPaceVO(Space space);

    /**
     * 构造space的查询条件
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * space分页查询
     * @param spaceQueryRequest
     * @return
     */
    SpacePageVO querySpacePage(SpaceQueryRequest spaceQueryRequest);

    /**
     * 校验当前空间容量
     * @param spaceId
     * @param picture
     */
    void checkUsage(Long spaceId, Picture picture, UploadPictureResult uploadPictureResult);

    void SpaceAuthCheck(Long spaceId, User loginUser);
}
