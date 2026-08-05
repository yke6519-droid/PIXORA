package com.example.picturebackend.Controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.picturebackend.Exception.ErrorCode;
import com.example.picturebackend.Exception.ThrowExceptionUtils;
import com.example.picturebackend.Service.PictureService;
import com.example.picturebackend.Service.UserService;
import com.example.picturebackend.Utils.ResponseUtils;
import com.example.picturebackend.annotation.AuthCheck;
import com.example.picturebackend.constant.UserConstant;
import com.example.picturebackend.domain.po.Picture;
import com.example.picturebackend.domain.po.PictureTagCategory;
import com.example.picturebackend.domain.po.User;
import com.example.picturebackend.domain.request.BaseResponse;
import com.example.picturebackend.domain.request.picture.*;
import com.example.picturebackend.domain.vo.PicturePageVO;
import com.example.picturebackend.domain.vo.PictureVO;

import com.example.picturebackend.manager.MultiCacheManager;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import ch.qos.logback.core.joran.util.beans.BeanUtil;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/picture")
public class PictureController {
    @Resource
    private PictureService pictureService;
    @Resource
    private UserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private MultiCacheManager multiCacheManager;

    /**
     * 构造本地缓存
     */
    private final Cache<String, String> LOCAL_CACHE= Caffeine.newBuilder()
            .initialCapacity(1024) // 初始化缓存容量
            .maximumSize(10_000L) //最大一万条数据
            .expireAfterWrite(Duration.ofMinutes(5)) // 写缓存后多久过期
            .build();

    /**
     * 上传图片
     * @param file 前端传递的文件（支持MultipartFile或String类型的URL）
     * @param request
     * @return
     */
    @PostMapping("/uploadPic")
    public BaseResponse<PictureVO> uploadPic(
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "url", required = false) String url,
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "tags", required = false) List<String> tags,
            @RequestParam(value = "introduction", required = false) String introduction,
            //todo 这里前端放置一个单选器：公共图库上传0L ， 私人图库：从loginUser中拿到spaceId传进来
            @RequestParam(value = "spaceId", required = false) Long spaceId,
            HttpServletRequest request) {

        User currentUser = userService.getCurrentUser(request);

        // 确定输入源：优先使用文件，其次使用URL
        Object inputSource = null;
        if (file != null && !file.isEmpty()) {
            inputSource = file;
        } else if (url != null && !url.isEmpty()) {
            inputSource = url;
        }
        // 判断是新增还是更新
        boolean isUpdate = id != null && id > 0;

        // 新增时必须传文件或URL；更新时可以不传（只改字段）
        if (!isUpdate && inputSource == null) {
            ThrowExceptionUtils.throwIF(true, ErrorCode.PARAMS_ERROR, "请选择要上传的图片文件或输入图片URL");
        }

        // 构建请求对象
        PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
        pictureUploadRequest.setId(id);
        pictureUploadRequest.setName(name);
        pictureUploadRequest.setCategory(category);
        pictureUploadRequest.setTags(tags);
        pictureUploadRequest.setIntroduction(introduction);
        pictureUploadRequest.setSpaceId(spaceId);
        
        // 上传图片
        PictureVO pictureVO = pictureService.uploadPicture2DB(inputSource, pictureUploadRequest, currentUser);
        return ResponseUtils.success(pictureVO);
    }

    /**
     * 管理员根据id删除图片
     */
    //    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @DeleteMapping("/deletePicture")
    public BaseResponse<Boolean> deletePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        ThrowExceptionUtils.throwIF(pictureUpdateRequest == null || pictureUpdateRequest.getId() == null,
                ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getCurrentUser(request);
        boolean result = pictureService.deletePictureById(pictureUpdateRequest.getId(), loginUser);
        return ResponseUtils.success(result);
    }

    /**
     * 管理员根据id更新图片
     */
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/updatePicture")
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        ThrowExceptionUtils.throwIF(pictureUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getCurrentUser(request);
        boolean result = pictureService.updatePictureById(pictureUpdateRequest, loginUser);
        return ResponseUtils.success(result);
    }

    /**
     * 分页获取图片列表
     */
    @PostMapping("/queryPicturePage")
    public BaseResponse<PicturePageVO> queryPicturePage(
            @RequestBody PictureQueryRequest pictureQueryRequest,
            HttpServletRequest request) {
        ThrowExceptionUtils.throwIF(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User currentUser = (User)request.getSession().getAttribute(UserConstant.CURRENT_USER_SESSION_KEY);
        PicturePageVO picturePageVO = new PicturePageVO();
        picturePageVO = pictureService.queryPicturePage(pictureQueryRequest,currentUser);
        return ResponseUtils.success(picturePageVO);
    }

    @PostMapping("/queryAll")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<IPage<Picture>> queryAll(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                 HttpServletRequest request) {
        User currentUser = (User) request.getSession().getAttribute(UserConstant.CURRENT_USER_SESSION_KEY);
        return ResponseUtils.success(pictureService.queryAll(pictureQueryRequest, currentUser));
    }

    /**
     * 通过缓存分页查询
     * 有分布式缓存 / 本地缓存两种方法
     * @param pictureQueryRequest 分页查询请求体
     * @param request HttpRequest
     * @return
     */
    @PostMapping("/queryPicturePageCache")
    public BaseResponse<PicturePageVO> queryPicturePageCache(
            @RequestBody PictureQueryRequest pictureQueryRequest,
            HttpServletRequest request) {
        ThrowExceptionUtils.throwIF(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User currentUser = (User)request.getSession().getAttribute(UserConstant.CURRENT_USER_SESSION_KEY);
        // 将多级缓存封装到一个工具类中，传入查询函数避免循环依赖
        PicturePageVO picturePageVO = multiCacheManager.getPicturePage(
                pictureQueryRequest, currentUser, pictureService::queryPicturePage);
        return ResponseUtils.success(picturePageVO);
    }

    /**
     * 根据id获取图片
     * 用户需要脱敏 管理员不需要
     */
    @GetMapping("/getPictureById")
    public BaseResponse<PictureVO> getPictureById(Long id,HttpServletRequest request) {
        ThrowExceptionUtils.throwIF(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        User currentUser = (User)request.getSession().getAttribute(UserConstant.CURRENT_USER_SESSION_KEY);
        PictureVO pictureVO = pictureService.getPictureById(id,currentUser);
        return ResponseUtils.success(pictureVO);
    }

    /**
     * 修改图片（仅图片所属用户可修改）
     */
    @PostMapping("/editPicture")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureUpdateRequest pictureUpdateRequest,
                                             HttpServletRequest request) {
        ThrowExceptionUtils.throwIF(pictureUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        User currentUser = userService.getCurrentUser(request);
        ThrowExceptionUtils.throwIF(currentUser == null, ErrorCode.NOT_LOGIN_ERROR);
        boolean result = pictureService.editPicture(pictureUpdateRequest, currentUser);
        return ResponseUtils.success(result);
    }

    /**
     * 管理员审核功能 根据图片id进行审核
     */
    @PutMapping("/adminCheckPicture")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> adminCheckPicture(@RequestBody AdminCheckPictureRequest adminCheckPictureRequest, HttpServletRequest request) {
        ThrowExceptionUtils.throwIF(
                ObjectUtil.isNull(adminCheckPictureRequest),
                ErrorCode.PARAMS_ERROR
        );
        User currentUser = userService.getCurrentUser(request);
        Boolean checkResult = pictureService.adminCheck(adminCheckPictureRequest, currentUser);
        return ResponseUtils.success(checkResult);
    }

    /**
     * 管理员审核功能 根据图片idBatch进行审核
     */
    @PutMapping("/adminCheckPictureBatch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> adminCheckPictureBatch(@RequestBody AdminCheckPictureBatchRequest adminCheckPictureBatchRequest, HttpServletRequest request) {
        ThrowExceptionUtils.throwIF(
                ObjectUtil.isNull(adminCheckPictureBatchRequest),
                ErrorCode.PARAMS_ERROR
        );
        User currentUser = userService.getCurrentUser(request);
        Boolean checkResult = pictureService.adminCheckBatch(adminCheckPictureBatchRequest, currentUser);
        return ResponseUtils.success(checkResult);
    }

    /**
     * 管理员批量拉取并上传图片
     * @param pictureUploadByBatchRequest
     * @param request
     * @return
     */
    @PostMapping("/adminFetchPictureBatch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<Picture>> adminFetchPictureBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,HttpServletRequest request){
        User currentUser = userService.getCurrentUser(request);
        ThrowExceptionUtils.throwIF(currentUser==null,ErrorCode.NOT_LOGIN_ERROR);
        
        List<Picture> pictures = pictureService.UploadPictureByBatch(pictureUploadByBatchRequest, currentUser);

        // int batchSize = pictureVOS.size();

        return ResponseUtils.success(pictures);
    }

    /**
     * 分类标签表
     * 目前种类较少，不选择存库
     * @return
     */
    @GetMapping("tag_category")
    public BaseResponse<PictureTagCategory> listPictureCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tags = Arrays.asList("热门", "搞笑", "艺术", "壁纸");
        List<String> categorys = Arrays.asList("模板", "表情包", "海报", "动漫", "游戏");
        pictureTagCategory.setTags(tags);
        pictureTagCategory.setCategorys(categorys);
        return ResponseUtils.success(pictureTagCategory);
    }
}
