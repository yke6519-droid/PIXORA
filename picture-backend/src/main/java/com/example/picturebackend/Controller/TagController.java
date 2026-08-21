package com.example.picturebackend.Controller;

import com.example.picturebackend.Exception.ErrorCode;
import com.example.picturebackend.Exception.ThrowExceptionUtils;
import com.example.picturebackend.Service.TagService;
import com.example.picturebackend.Service.UserService;
import com.example.picturebackend.Utils.ResponseUtils;
import com.example.picturebackend.annotation.AuthCheck;
import com.example.picturebackend.constant.UserConstant;
import com.example.picturebackend.domain.po.Tag;
import com.example.picturebackend.domain.po.User;
import com.example.picturebackend.domain.request.BaseResponse;
import com.example.picturebackend.domain.request.tag.TagCreateRequest;
import com.example.picturebackend.domain.request.tag.TagIdRequest;
import com.example.picturebackend.domain.request.tag.TagRenameRequest;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 空间标签及管理员审核接口。 */
@RestController
@RequestMapping("/tag")
public class TagController {

    @Resource
    private TagService tagService;

    @Resource
    private UserService userService;

    /**
     * 查询可用于图片筛选和绑定的标签。
     * Controller 固定只查可用标签，不把停用状态开关交给前端决定。
     */
    @GetMapping("/list")
    public BaseResponse<List<Tag>> list(@RequestParam("spaceId") Long spaceId,
                                        HttpServletRequest request) {
        User loginUser = userService.getCurrentUser(request);
        return ResponseUtils.success(tagService.listBySpaceId(spaceId, false, loginUser));
    }

    /** 查询空间的全部标签，供空间标签管理和管理员审核页面使用。 */
    @GetMapping("/manage/list")
    public BaseResponse<List<Tag>> listForManagement(@RequestParam("spaceId") Long spaceId,
                                                     HttpServletRequest request) {
        User loginUser = userService.getCurrentUser(request);
        return ResponseUtils.success(tagService.listBySpaceId(spaceId, true, loginUser));
    }

    /** 创建空间标签。具体的空间持有人权限和标签额度由 Service 校验。 */
    @PostMapping("/create")
    public BaseResponse<Tag> create(@RequestBody TagCreateRequest tagCreateRequest,
                                    HttpServletRequest request) {
        ThrowExceptionUtils.throwIF(tagCreateRequest == null,
                ErrorCode.PARAMS_ERROR, "请求体不能为空");
        User loginUser = userService.getCurrentUser(request);
        Tag tag = tagService.createTag(
                tagCreateRequest.getSpaceId(),
                tagCreateRequest.getTagName(),
                loginUser);
        return ResponseUtils.success(tag);
    }

    /** 修改标签名称，图片与标签的 id 绑定关系保持不变。 */
    @PutMapping("/rename")
    public BaseResponse<Boolean> rename(@RequestBody TagRenameRequest tagRenameRequest,
                                         HttpServletRequest request) {
        ThrowExceptionUtils.throwIF(tagRenameRequest == null,
                ErrorCode.PARAMS_ERROR, "请求体不能为空");
        User loginUser = userService.getCurrentUser(request);
        Boolean result = tagService.renameTag(
                tagRenameRequest.getTagId(),
                tagRenameRequest.getTagName(),
                loginUser);
        return ResponseUtils.success(result);
    }

    /** 删除标签实体，同时解除该标签的全部图片关联。 */
    @DeleteMapping("/delete")
    public BaseResponse<Boolean> delete(@RequestBody TagIdRequest tagIdRequest,
                                        HttpServletRequest request) {
        ThrowExceptionUtils.throwIF(tagIdRequest == null,
                ErrorCode.PARAMS_ERROR, "请求体不能为空");
        User loginUser = userService.getCurrentUser(request);
        return ResponseUtils.success(tagService.deleteTag(tagIdRequest.getTagId(), loginUser));
    }

    /** 管理员停用标签；历史关联保留，但标签不再参与新绑定和正常展示。 */
    @PutMapping("/disable")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> disable(@RequestBody TagIdRequest tagIdRequest,
                                         HttpServletRequest request) {
        ThrowExceptionUtils.throwIF(tagIdRequest == null,
                ErrorCode.PARAMS_ERROR, "请求体不能为空");
        User adminUser = userService.getCurrentUser(request);
        return ResponseUtils.success(tagService.disableTag(tagIdRequest.getTagId(), adminUser));
    }

    /** 管理员恢复标签；恢复前由 Service 校验图片标签数量上限。 */
    @PutMapping("/restore")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> restore(@RequestBody TagIdRequest tagIdRequest,
                                         HttpServletRequest request) {
        ThrowExceptionUtils.throwIF(tagIdRequest == null,
                ErrorCode.PARAMS_ERROR, "请求体不能为空");
        User adminUser = userService.getCurrentUser(request);
        return ResponseUtils.success(tagService.restoreTag(tagIdRequest.getTagId(), adminUser));
    }
}
