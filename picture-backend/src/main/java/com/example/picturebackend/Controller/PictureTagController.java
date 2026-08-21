package com.example.picturebackend.Controller;

import com.example.picturebackend.Exception.ErrorCode;
import com.example.picturebackend.Exception.ThrowExceptionUtils;
import com.example.picturebackend.Service.PictureTagService;
import com.example.picturebackend.Service.UserService;
import com.example.picturebackend.Utils.ResponseUtils;
import com.example.picturebackend.domain.po.User;
import com.example.picturebackend.domain.request.BaseResponse;
import com.example.picturebackend.domain.request.picture.PictureTagBatchRequest;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 图片与空间标签关联接口。 */
@RestController
@RequestMapping("/pictureTag")
public class PictureTagController {

    @Resource
    private PictureTagService pictureTagService;

    @Resource
    private UserService userService;

    /** 批量给图片添加标签；任意图片超过 3 个有效标签时整批失败。 */
    @PostMapping("/add")
    public BaseResponse<Boolean> add(@RequestBody PictureTagBatchRequest batchRequest,
                                     HttpServletRequest request) {
        ThrowExceptionUtils.throwIF(batchRequest == null,
                ErrorCode.PARAMS_ERROR, "请求体不能为空");
        User loginUser = userService.getCurrentUser(request);
        Boolean result = pictureTagService.addTags(
                batchRequest.getSpaceId(),
                batchRequest.getPictureIds(),
                batchRequest.getTagIds(),
                loginUser);
        return ResponseUtils.success(result);
    }

    /** 批量移除图片标签，只删除关联关系，不释放空间标签额度。 */
    @PostMapping("/remove")
    public BaseResponse<Boolean> remove(@RequestBody PictureTagBatchRequest batchRequest,
                                        HttpServletRequest request) {
        ThrowExceptionUtils.throwIF(batchRequest == null,
                ErrorCode.PARAMS_ERROR, "请求体不能为空");
        User loginUser = userService.getCurrentUser(request);
        Boolean result = pictureTagService.removeTags(
                batchRequest.getSpaceId(),
                batchRequest.getPictureIds(),
                batchRequest.getTagIds(),
                loginUser);
        return ResponseUtils.success(result);
    }
}
