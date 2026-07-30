package com.example.picturebackend.Controller;
import cn.hutool.core.util.ObjectUtil;
import com.example.picturebackend.Exception.ErrorCode;
import com.example.picturebackend.Exception.ThrowExceptionUtils;
import com.example.picturebackend.Service.SpaceService;
import com.example.picturebackend.Service.UserService;
import com.example.picturebackend.Utils.ResponseUtils;
import com.example.picturebackend.annotation.AuthCheck;
import com.example.picturebackend.constant.UserConstant;
import com.example.picturebackend.domain.po.Space;
import com.example.picturebackend.domain.po.User;
import com.example.picturebackend.domain.request.BaseResponse;
import com.example.picturebackend.domain.request.space.AlterLevelRequest;
import com.example.picturebackend.domain.request.space.CreateSpaceRequest;
import com.example.picturebackend.domain.request.space.SpaceQueryRequest;
import com.example.picturebackend.domain.request.space.SpaceUpdateRequest;
import com.example.picturebackend.domain.vo.SpacePageVO;
import com.example.picturebackend.domain.vo.SpaceVO;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/space")
public class SpaceController {
    @Resource
    private SpaceService spaceService;
    @Resource
    private UserService userService;
    //1. 空间的增删改查

    /**
     * 根据条件单个查找
     * 仅 创建者和管理员可查
     * @param spaceId
     * @param request
     * @return
     */
    @GetMapping("/querySpaceById")
    public BaseResponse<SpaceVO> querySpaceById(Long spaceId, HttpServletRequest request) {
        ThrowExceptionUtils.throwIF(ObjectUtil.isNull(spaceId), ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getCurrentUser(request);
        SpaceVO spaceVO = spaceService.querySpace(spaceId, loginUser);
        return ResponseUtils.success(spaceVO);
    }

    /**
     * Space分页查询
     * 仅管理员可查
     * @param spaceQueryRequest
     * @return
     */
    @GetMapping("/querySpacePage")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<SpacePageVO> querySpacePage(SpaceQueryRequest spaceQueryRequest) {
        ThrowExceptionUtils.throwIF(ObjectUtil.isNull(spaceQueryRequest), ErrorCode.PARAMS_ERROR);
        SpacePageVO spacePageVO = spaceService.querySpacePage(spaceQueryRequest);
        return ResponseUtils.success(spacePageVO);
    }

    /**
     * 用户创建空间
     * @param createSpaceRequest
     * @param request
     * @return
     */
    @PostMapping("/createSpace")
    public BaseResponse<Space> createSpace(@RequestBody CreateSpaceRequest createSpaceRequest, HttpServletRequest request){
        ThrowExceptionUtils.throwIF(ObjectUtil.isNull(createSpaceRequest),ErrorCode.PARAMS_ERROR,"请求体为空");
        User loginUser = userService.getCurrentUser(request);
        Space createdSpace = spaceService.createSpace(createSpaceRequest, loginUser);
        return ResponseUtils.success(createdSpace);
    }

    /**
     * 根据id删除空间
     * 仅 创建者和管理员
     * @param spaceId
     * @param request
     * @return
     */
    @DeleteMapping("/deleteById")
    public BaseResponse<Boolean> deleteById(Long spaceId, HttpServletRequest request){
        ThrowExceptionUtils.throwIF(ObjectUtil.isNull(spaceId),ErrorCode.PARAMS_ERROR,"空间id为空");
        User loginUser = userService.getCurrentUser(request);
        Boolean res = spaceService.deleteById(spaceId,loginUser);
        return ResponseUtils.success(res);
    }

    /**
     * 根据id更新空间信息
     * 仅 创建者和管理员
     * @param spaceUpdateRequest
     * @param request
     * @return
     */
    @PutMapping("/updateById")
    public BaseResponse<Boolean> updateById(@RequestBody SpaceUpdateRequest spaceUpdateRequest, HttpServletRequest request){
        ThrowExceptionUtils.throwIF(ObjectUtil.isNull(spaceUpdateRequest),ErrorCode.PARAMS_ERROR,"空间id为空");
        User loginUser = userService.getCurrentUser(request);
        Boolean res = spaceService.updateById(spaceUpdateRequest,loginUser);
        return ResponseUtils.success(res);
    }

    /**
     * 根据id修改空间等级
     * 目前：仅管理员可以修改
     * 后续可以允许普通用户升级空间等级
     * @param alterLevelRequest
     * @param request
     * @return
     */
    @PutMapping("/alterLevelById")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> alterLevelById(@RequestBody AlterLevelRequest alterLevelRequest, HttpServletRequest request){
        ThrowExceptionUtils.throwIF(ObjectUtil.isNull(alterLevelRequest),ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getCurrentUser(request);
        boolean b = spaceService.alterLevelById(alterLevelRequest, loginUser);
        return ResponseUtils.success(b);
    }
}
