package com.example.picturebackend.Controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.BeanUtils;
import com.example.picturebackend.Exception.BusinessException;
import com.example.picturebackend.Exception.ErrorCode;
import com.example.picturebackend.Exception.ThrowExceptionUtils;
import com.example.picturebackend.Service.PictureService;
import com.example.picturebackend.Service.UserService;
import com.example.picturebackend.Utils.ResponseUtils;
import com.example.picturebackend.annotation.AuthCheck;
import com.example.picturebackend.constant.PictureConstant;
import com.example.picturebackend.constant.UserConstant;
import com.example.picturebackend.domain.po.Picture;
import com.example.picturebackend.domain.po.PictureTagCategory;
import com.example.picturebackend.domain.po.User;
import com.example.picturebackend.domain.request.BaseResponse;
import com.example.picturebackend.domain.request.picture.*;
import com.example.picturebackend.domain.vo.picture.PictureListVO;
import com.example.picturebackend.domain.vo.picture.PicturePageVO;
import com.example.picturebackend.domain.vo.picture.PictureUploadFailVO;
import com.example.picturebackend.domain.vo.picture.PictureUploadVO;
import com.example.picturebackend.domain.vo.picture.PictureVO;
import com.example.picturebackend.domain.vo.user.UserVO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;


import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Slf4j
@RestController
@RequestMapping("/picture")
public class PictureController {
    /** 
     * 批量本地上传时，所有文件内容的总大小上限（不含 multipart 边界和文本字段）。 
     * */
    private static final long MAX_BATCH_TOTAL_SIZE = 30L * 1024 * 1024;

    @Resource
    private PictureService pictureService;
    @Resource
    private UserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
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
    @PostMapping(value = "/uploadPic", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<PictureUploadVO> uploadPic(
            @RequestPart(value = "fileList", required = false) List<MultipartFile> fileList,
            @RequestParam(value = "url", required = false) String url,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "tags", required = false) List<String> tags,
            @RequestParam(value = "introduction", required = false) String introduction,
            @RequestParam(value = "spaceId", required = false) Long spaceId,
            HttpServletRequest request) {

        User currentUser = userService.getCurrentUser(request);


        // 构建请求对象
        PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
        pictureUploadRequest.setName(name);
        pictureUploadRequest.setCategory(category);
        pictureUploadRequest.setTags(tags);
        pictureUploadRequest.setIntroduction(introduction);
        pictureUploadRequest.setSpaceId(spaceId);
        
        Integer totalSize = 0;
        List<PictureVO> successPictureVOs = new ArrayList<>();
        List<PictureUploadFailVO> failPictureVOs = new ArrayList<>();
        
        boolean hasFiles = fileList != null && !fileList.isEmpty();
        boolean hasUrl = url != null && !url.isBlank();

        // 文件和 URL 是两种互斥输入来源，避免客户端误传时静默忽略其中一种。
        ThrowExceptionUtils.throwIF(
                hasFiles && hasUrl,
                ErrorCode.PARAMS_ERROR,
                "本地文件和图片 URL 不能同时上传"
        );

        if (hasFiles) {

            // 批次上传容量校验
            Long totalFileSize = 0L;
            for (MultipartFile file : fileList) {
                ThrowExceptionUtils.throwIF(
                        file == null || file.isEmpty(),
                        ErrorCode.PARAMS_ERROR,
                        "上传文件不能为空"
                );
                totalFileSize += file.getSize();
                totalSize++;
            }
            ThrowExceptionUtils.throwIF(
                    totalFileSize > MAX_BATCH_TOTAL_SIZE,
                    ErrorCode.PARAMS_ERROR,
                    "批量上传图片总大小不能超过30MB"
            );

            // 开始上传
            for (MultipartFile file : fileList) {
                try {
                    // 上传图片
                    PictureVO pictureVO = pictureService.uploadPicture2DB(file, pictureUploadRequest, currentUser);
                    // 成功
                    successPictureVOs.add(pictureVO);
                } catch (BusinessException e) {

                    // 封装失败原因
                    failPictureVOs.add(new PictureUploadFailVO(
                        // index,
                        file.getSize(),
                        file.getOriginalFilename(),
                        e.getMessage()));
                }
            }

        } else if (hasUrl) {
            totalSize++;
            PictureVO pictureVO = pictureService.uploadPicture2DB(url, pictureUploadRequest, currentUser);
            successPictureVOs.add(pictureVO);
        } else {
            ThrowExceptionUtils.throwIF(
                    true,
                    ErrorCode.PARAMS_ERROR,
                    "请选择要上传的图片文件或输入图片 URL"
            );
        }

        // 构造vo
        PictureUploadVO pictureUploadVO = new PictureUploadVO();
        pictureUploadVO.setSuccessPictureList(successPictureVOs);
        pictureUploadVO.setSuccessCount(successPictureVOs.size());
        pictureUploadVO.setFailPictureList(failPictureVOs);
        pictureUploadVO.setFailCount(failPictureVOs.size());
        pictureUploadVO.setTotalCount(totalSize);
        
        return ResponseUtils.success(pictureUploadVO);
    }

    /**
     * 重新上传图片（能够更新原图片，或单独修改元信息）
     * @param file
     * @param pictureId
     * @param name
     * @param category
     * @param tags
     * @param introduction
     * @param request
     * @return
     */
    @PostMapping("/reloadPicture")
    public BaseResponse<PictureVO> reloadPicture(
        @RequestPart(value = "file", required = false) MultipartFile file,
        @RequestParam(value = "url", required = false) String url,
        @RequestParam(value = "id", required = false) Long pictureId,
        @RequestParam(value = "name", required = false) String name,
        @RequestParam(value = "category", required = false) String category,
        @RequestParam(value = "tags", required = false) List<String> tags,
        @RequestParam(value = "introduction", required = false) String introduction,
        HttpServletRequest request){
        
        User currentUser = userService.getCurrentUser(request);
        
        boolean hasFile = file != null;
        boolean hasUrl = url != null && !url.isBlank();

        // 文件和 URL 是两种互斥输入来源，避免客户端误传时静默忽略其中一种。
        ThrowExceptionUtils.throwIF(
                hasFile && hasUrl,
                ErrorCode.PARAMS_ERROR,
                "本地文件和图片 URL 不能同时上传"
        );

        // 判断是url还是本地file
        Object inputSource = null;
        if (hasFile) {
            inputSource = file;
        } else if (hasUrl) {
            inputSource = url;
        }
        
        // 参数校验
        ThrowExceptionUtils.throwIF(ObjectUtil.isNull(pictureId), 
            ErrorCode.PARAMS_ERROR, 
            "图片ID不能为空");
        
        Picture oldPicture = pictureService.getById(pictureId);
        ThrowExceptionUtils.throwIF(ObjectUtil.isNull(oldPicture), 
            ErrorCode.PARAMS_ERROR, 
            "目标图片不存在");
        
        ThrowExceptionUtils.throwIF(
            !oldPicture.getPictureCheck().equals(PictureConstant.CHECK_REFUSE),
            ErrorCode.PARAMS_ERROR,
            "只能重新上传审核拒绝的图片");
        
        pictureService.PictureAuthCheck(currentUser,pictureService.getById(pictureId));
        
        // 构造请求体
        PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
        pictureUploadRequest.setId(pictureId);
        pictureUploadRequest.setName(name);
        pictureUploadRequest.setCategory(category);
        pictureUploadRequest.setTags(tags);
        pictureUploadRequest.setIntroduction(introduction);

        // 上传图片
        PictureVO pictureVO = pictureService.reloadPicture(inputSource, pictureUploadRequest, currentUser);

        return ResponseUtils.success(pictureVO);
    }

    /**
     * 管理员根据id删除图片
     */
    @DeleteMapping("/deletePicture")
    public BaseResponse<Boolean> deletePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        
        // 参数校验
        ThrowExceptionUtils.throwIF(pictureUpdateRequest == null || pictureUpdateRequest.getId() == null,
                ErrorCode.PARAMS_ERROR);
        
        User loginUser = userService.getCurrentUser(request);
        
        // 执行删除
        boolean result = pictureService.deletePictureById(pictureUpdateRequest.getId(), loginUser);

        return ResponseUtils.success(result);
    }

    /**
     * 管理员根据id更新图片
     */
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/updatePicture")
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        // 参数校验
        ThrowExceptionUtils.throwIF(pictureUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取当前登录用户
        User loginUser = userService.getCurrentUser(request);
        // 调用服务层方法更新图片
        boolean result = pictureService.updatePictureById(pictureUpdateRequest, loginUser);
        // 返回结果
        return ResponseUtils.success(result);
    }

    /**
     * 分页获取图片列表
     * 允许未登录用户访问，但是有限制
     */
    @PostMapping("/queryPicturePage")
    public BaseResponse<PicturePageVO> queryPicturePage(
            @RequestBody PictureQueryRequest pictureQueryRequest,
            HttpServletRequest request) {
        // 校验
        ThrowExceptionUtils.throwIF(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        normalizePublicSpaceId(pictureQueryRequest);

        // 允许未登录用户
        UserVO currentUser = (UserVO)request.getSession().getAttribute(UserConstant.CURRENT_USER_SESSION_KEY);
        
        
        
        // 若currentUser为空，则对应传入的是一个空参的User
        User user= new User();
        BeanUtil.copyProperties(currentUser, user);
        
        PicturePageVO picturePageVO = new PicturePageVO();
        picturePageVO = pictureService.queryPicturePage(pictureQueryRequest,user);

        return ResponseUtils.success(picturePageVO);
    }

    @PostMapping("/queryAll")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<IPage<PictureVO>> queryAll(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                 HttpServletRequest request) {

        UserVO currentUser = (UserVO) request.getSession().getAttribute(UserConstant.CURRENT_USER_SESSION_KEY);
        User user= new User();
        BeanUtil.copyProperties(currentUser, user);
        
        IPage<PictureVO> picturIPage = pictureService.queryAll(pictureQueryRequest, user);

        return ResponseUtils.success(picturIPage);
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
        // 先拦截空请求体，避免归一化spaceId时发生空指针；真正的空间权限校验仍由Service负责。
        ThrowExceptionUtils.throwIF(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR, "请求体为空");
        normalizePublicSpaceId(pictureQueryRequest);

        UserVO currentUser = (UserVO)request.getSession().getAttribute(UserConstant.CURRENT_USER_SESSION_KEY);
        
        User user= new User();
        BeanUtil.copyProperties(currentUser, user);
        
        // 重要：缓存入口必须交给 Service 处理，由 Service 先校验私有空间权限，再读取缓存。
        // 如果 Controller 直接读取缓存，命中缓存时就会跳过原本只在数据库查询前执行的权限校验。
        PicturePageVO picturePageVO = pictureService.queryPicturePageCache(pictureQueryRequest, user);

        return ResponseUtils.success(picturePageVO);
    }

    /**
     * 公共图库前端统一传0；兼容历史客户端未传spaceId的请求，按公共图库处理。
     * 统一在进入缓存和业务层前归一化，避免null形成另一套缓存键或查询到私人空间图片。
     */
    private void normalizePublicSpaceId(PictureQueryRequest pictureQueryRequest) {
        if (pictureQueryRequest.getSpaceId() == null) {
            pictureQueryRequest.setSpaceId(0L);
        }
    }

    /**
     * 根据id获取图片
     * 用户需要脱敏 管理员不需要
     */
    @GetMapping("/getPictureById")
    public BaseResponse<PictureVO> getPictureById(Long id,HttpServletRequest request) {
        ThrowExceptionUtils.throwIF(id == null || id <= 0, ErrorCode.PARAMS_ERROR);

        UserVO currentUser = (UserVO)request.getSession().getAttribute(UserConstant.CURRENT_USER_SESSION_KEY);
        
        User user= new User();
        BeanUtil.copyProperties(currentUser, user);

        PictureVO pictureVO = pictureService.getPictureById(id,user);
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
     * todo 这里最好加上一个事务处理，防止最终上传失败，导致部分图片上传成功，部分失败
     */
    @PostMapping("/adminFetchPictureBatch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureListVO> adminFetchPictureBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,HttpServletRequest request){
        User currentUser = userService.getCurrentUser(request);
        ThrowExceptionUtils.throwIF(currentUser==null,ErrorCode.NOT_LOGIN_ERROR);
        
        PictureListVO pictureListVO = pictureService.UploadPictureByBatch(pictureUploadByBatchRequest, currentUser);

        return ResponseUtils.success(pictureListVO);
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
