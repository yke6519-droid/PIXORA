package com.example.picturebackend.Service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.picturebackend.Exception.BusinessException;
import com.example.picturebackend.Exception.ErrorCode;
import com.example.picturebackend.Exception.ThrowExceptionUtils;
import com.example.picturebackend.Service.PictureService;
import com.example.picturebackend.Service.SpaceService;
import com.example.picturebackend.Service.UserService;
import com.example.picturebackend.Mapper.PictureMapper;
import com.example.picturebackend.constant.PictureConstant;
import com.example.picturebackend.constant.UserConstant;
import com.example.picturebackend.domain.dto.file.UploadPictureResult;
import com.example.picturebackend.domain.po.Picture;
import com.example.picturebackend.domain.po.Space;
import com.example.picturebackend.domain.po.User;
import com.example.picturebackend.domain.request.picture.*;
import com.example.picturebackend.domain.vo.PicturePageVO;
import com.example.picturebackend.domain.vo.PictureVO;
import com.example.picturebackend.manager.MultiCacheManager;
import com.example.picturebackend.manager.upload.FilePictureUpload;
import com.example.picturebackend.manager.upload.PictureUploadTemplate;
import com.example.picturebackend.manager.upload.UrlPictureUpload;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author chen
* @description 针对表【picture(图库表)】的数据库操作Service实现
* @createDate 2026-04-28 18:35:01
*/
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{
    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private UserService userService;

    @Resource
    private MultiCacheManager multiCacheManager;

    private static final Gson GSON = new Gson();

    @Autowired
    @Resource
    private SpaceService spaceService;

    /**
     * 校验当前登录用户是否是目标空间的持有人
     */
    @Override
    public Boolean SpaceCheck(Long userId, User loginUser) {
        //1. 校验空间是否存在 和 用户是否登录
        ThrowExceptionUtils.throwIF(ObjectUtil.isNull(userId), ErrorCode.PARAMS_ERROR);
        ThrowExceptionUtils.throwIF(ObjectUtil.isNull(loginUser), ErrorCode.NOT_LOGIN_ERROR);
        //2. 校验传入loginUser是否是该space的持有者 或 当前登录用户是否是管理员
        ThrowExceptionUtils.throwIF(!userId.equals(loginUser.getId()) &&
                        !loginUser.getUserstatus().equals(UserConstant.ADMIN_ROLE),
                ErrorCode.NO_AUTH_ERROR);
        return true;
    }

    /**
     * 上传图片到存储对象
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return 返回图片解析结果对象
     */
    @Override
    public UploadPictureResult uploadPicture2COS(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 校验参数
        ThrowExceptionUtils.throwIF(
                loginUser == null,
                ErrorCode.NOT_LOGIN_ERROR
        );
        System.out.println(inputSource);
        System.out.println(pictureUploadRequest);
        ThrowExceptionUtils.throwIF(
                inputSource == null || pictureUploadRequest == null,
                ErrorCode.PARAMS_ERROR,"数据不能为空"
        );
        // 判断是新增还是更新
        Long pictureId = pictureUploadRequest.getId();
        // 若上传了图片id则视为更新
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowExceptionUtils.throwIF(oldPicture==null,ErrorCode.PARAMS_ERROR,"图片不存在");
            ThrowExceptionUtils.throwIF(!loginUser.getId().equals(oldPicture.getUserid()),ErrorCode.NO_AUTH_ERROR,"仅用户本人可以修改图片");
        }
        // 上传图库
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        // 按照用户id划分目录
        if (pictureUploadRequest.getSpaceId() != null) {
            uploadPathPrefix = String.format("userSpace/%s", loginUser.getId());
        }
        // 获取到要上传的图片的原始信息
        // 根据inputSource的类型，使用不同的上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String){
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        return uploadPictureResult;
    }

    /**
     * 上传图片 后端自动校验用户角色
     * 管理员 自动审核通过
     * 普通用户 待审核
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return PictureVO
     */
    @Override
    @Transactional
    public PictureVO uploadPicture2DB(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 获取目标图片id
        Long picId = pictureUploadRequest.getId();
        Picture picture = new Picture();
        // 先根据是否上传id 判断更新还是创建
        if (picId!=null){ //更新
            //1.是否更新图片
            if (inputSource!=null){ //1.1 更新则重新上传并解析
                UploadPictureResult uploadPictureResult = this.uploadPicture2COS(inputSource, pictureUploadRequest, loginUser);
                BeanUtils.copyProperties(uploadPictureResult,picture);
            }else { //1.2 不更新图片，根据id拿到原本的数据，再用请求中的新信息覆盖
                Picture oldPic = this.getById(picId);
                BeanUtils.copyProperties(oldPic,picture);
            }
            // 用原图id，目的是在原图上进行更新
            picture.setId(picId);
        }
        else { //创建，将图片解析出的信息给picture
            UploadPictureResult uploadPictureResult = this.uploadPicture2COS(inputSource, pictureUploadRequest, loginUser);
            BeanUtils.copyProperties(uploadPictureResult,picture);
            picture.setUserid(loginUser.getId());
        }
        // 用请求中的新信息覆盖旧数据
        if (pictureUploadRequest.getName() != null) {
            picture.setName(pictureUploadRequest.getName());
        }
        if (pictureUploadRequest.getCategory() != null) {
            picture.setCategory(pictureUploadRequest.getCategory());
        }
        if (pictureUploadRequest.getTags() != null) {
            picture.setTags(JSONUtil.toJsonStr(pictureUploadRequest.getTags()));
        }
        if (pictureUploadRequest.getIntroduction() != null) {
            picture.setIntroduction(pictureUploadRequest.getIntroduction());
        }
        // 加入空间的校验
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) { //指定了空间
            //1. 先校验操作者是否是空间持有人
            Space space = spaceService.getById(spaceId);
            Long userId = space.getUserId();
            this.SpaceCheck(userId, loginUser);
            //2. 校验该空间内是否还有容量
            Long maxCount = space.getMaxCount();
            Long usedCount = space.getUsedCount();
            Long maxSize = space.getMaxSize();
            Long usedSize = space.getUsedSize();
            ThrowExceptionUtils.throwIF(Objects.equals(usedCount, maxCount), ErrorCode.PARAMS_ERROR, "空间图片数已达上限");
            ThrowExceptionUtils.throwIF(usedSize + picture.getPicsize() > maxSize ||
                            usedSize.equals(maxSize)
                    , ErrorCode.PARAMS_ERROR, "空间容量已达上限");
            //3. 校验成功后 给图片设置spaceId
            picture.setSpaceId(spaceId);
            //4. 将space的容量和图片数进行更新（这里可以先更新，因为开启了事务回滚，如果后续Sql操作出错，这里会回滚）
            space.setUsedCount(usedCount + 1);
            space.setUsedSize(usedSize + picture.getPicsize());
            spaceService.updateById(space);
        } else {
            picture.setSpaceId(0L);
        }
        // 设置上传者id
        picture.setUserid(loginUser.getId());
        // 管理员自动过审
        if (loginUser.getUserstatus().equals(UserConstant.ADMIN_ROLE)) {
            // 填充审核信息 - 管理员自动过审 - 管理员修改后也不需要进入待审核
            picture.setPictureCheck(PictureConstant.CHECK_PASS);
            picture.setCheckAdminId(loginUser.getId());
            picture.setCheckTime(DateTime.now());
        } else {
            // 无论更新还是创建后,都需要设置待审核状态
            picture.setPictureCheck(PictureConstant.CHECK_AWAIT);
        }
        // 存入数据并返回VO
        boolean result = this.saveOrUpdate(picture);
        ThrowExceptionUtils.throwIF(
                !result,
                ErrorCode.OPERATION_ERROR,
                "图片上传失败！"
        );
        // 清除缓存，保证数据一致性
        multiCacheManager.invalidatePicturePageCache();
        return PictureVO.obj2VO(picture);
    }

    /**
     * 根据id删除图片
     */
    @Override
    @Transactional
    public Boolean deletePictureById(Long id, User loginUser) {
        ThrowExceptionUtils.throwIF(id <= 0, ErrorCode.PARAMS_ERROR, "图片id非法");
        Picture picture = this.getById(id);
        ThrowExceptionUtils.throwIF(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        //1. 判断图片是否在私人空间内
        Long spaceId = picture.getSpaceId();
        if (spaceId != 0) {
            // 校验当前用户是否有权操作空间中图片
            this.SpaceCheck(spaceId, loginUser);
            //1. 校验完成后 先删除图片
            boolean result = this.removeById(id);
            //2. 再更新空间的容量
            Space space = spaceService.getById(spaceId);
            space.setUsedSize(space.getUsedSize() - picture.getPicsize());
            space.setUsedCount(space.getUsedCount() - 1L);
            spaceService.updateById(space);
            // 清除缓存，保证数据一致性
            if (result) {
                multiCacheManager.invalidatePicturePageCache();
            }
            return result;
        }
        // 仅管理员和图片所属者可以操作
        AuthCheck(loginUser, picture);
        boolean result = this.removeById(id);
        // 清除缓存，保证数据一致性
        if (result) {
            multiCacheManager.invalidatePicturePageCache();
        }
        return result;
    }

    /**
     * 根据id更新图片信息
     */
    @Override
    public Boolean updatePictureById(PictureUpdateRequest pictureUpdateRequest, User loginUser) {
        ThrowExceptionUtils.throwIF(pictureUpdateRequest == null || pictureUpdateRequest.getId() == null,
                ErrorCode.PARAMS_ERROR);
        Picture picture = this.getById(pictureUpdateRequest.getId());
        ThrowExceptionUtils.throwIF(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        //1. 判断图片是否在私人空间内
        Long spaceId = picture.getSpaceId();
        if (spaceId != 0) {
            // 校验当前用户是否有权操作空间中图片
            this.SpaceCheck(spaceId, loginUser);
        }
        // 仅管理员和图片所属者可以操作
        AuthCheck(loginUser, picture);
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, updatePicture);
        String tagStr = JSONUtil.toJsonStr(pictureUpdateRequest.getTags());
        updatePicture.setTags(tagStr);
        updatePicture.setUpdatetime(new Date());
        boolean result = this.updateById(updatePicture);
        // 清除缓存，保证数据一致性
        if (result) {
            multiCacheManager.invalidatePicturePageCache();
        }
        return result;
    }

    /**
     * 无论是不是私人空间 或是待审核 的图片 管理员均能查看
     *
     * @param adminUser
     * @return
     */
    @Override
    public IPage<Picture> queryAll(PictureQueryRequest pictureQueryRequest, User adminUser) {
        // 仅管理员可用的后门
        ThrowExceptionUtils.throwIF(!adminUser.getUserstatus().equals(UserConstant.ADMIN_ROLE),
                ErrorCode.NO_AUTH_ERROR);
        ThrowExceptionUtils.throwIF(ObjectUtil.isNull(pictureQueryRequest), ErrorCode.PARAMS_ERROR);
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        String searchText = pictureQueryRequest.getSearchText();
        List<String> tags = pictureQueryRequest.getTags();
        Long userId = pictureQueryRequest.getUserId();
        Integer current = pictureQueryRequest.getCurrent();
        Integer pageSize = pictureQueryRequest.getPageSize();
        String sortFiled = pictureQueryRequest.getSortFiled();
        String sortOrder = pictureQueryRequest.getSortOrder();
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        /**
         * 用searchText从多字段中筛选
         */
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText));
        }
        queryWrapper.eq(ObjectUtil.isNotNull(id), "id", id);
        queryWrapper.eq(ObjectUtil.isNotNull(userId), "userId", userId);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortFiled), sortOrder.equals("ascend"), sortFiled);
        // 接收到List<String>格式的tags，转化为对JSON的查询
        if (CollUtil.isNotEmpty(tags)) {
            queryWrapper.and(pictureQueryWrapper -> {
                for (String tag : tags) {
                    pictureQueryWrapper.apply("JSON_VALID(tags) AND JSON_CONTAINS(tags, JSON_QUOTE({0}))", tag);
                }
            });
        }
        return this.page(new Page<Picture>(current, pageSize), queryWrapper);
    }

    /**
     * 分页获取图片列表
     * spaceId传入null 查询公共图库
     * spaceId传入loginUser.spaceId 查询对应用户的图库
     */
    @Override
    public PicturePageVO queryPicturePage(PictureQueryRequest pictureQueryRequest, User currentUser) {
        ThrowExceptionUtils.throwIF(ObjectUtil.isNull(pictureQueryRequest),
                ErrorCode.PARAMS_ERROR,"请求体为空");
        Long spaceId = pictureQueryRequest.getSpaceId();
        if (spaceId > 0L) { // 若请求体中传入的空间id
            // 校验当前登录用户是否是空间持有人
            Space space = spaceService.getById(spaceId);
            ThrowExceptionUtils.throwIF(!space.getUserId().equals(currentUser.getId()),
                    ErrorCode.NO_AUTH_ERROR, "无法查看他人空间中的内容");
        }
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制普通用户和未登录用户的分页上限
        if (ObjectUtil.isNull(currentUser) || !currentUser.getUserstatus().equals(UserConstant.ADMIN_ROLE)){
            if (size > 10){
                size = 10;
            }
        }
        // 构建查询条件
        QueryWrapper<Picture> queryWrapper = getQueryWrapper(pictureQueryRequest);
        // 分页查询
        Page<Picture> picturePage = this.page(new Page<>(current, size), queryWrapper);
        // 转换为VO列表
        List<PictureVO> pictureVOList = picturePage.getRecords().stream()
                .map(picture -> {
                    PictureVO pictureVO = getPictureVO(picture);
                    User createdUser = userService.getById(picture.getUserid());
                    // 若为普通用户 则对创建人信息进行脱敏处理
                    if (ObjectUtil.isNull(currentUser) || !currentUser.getUserstatus().equals(UserConstant.ADMIN_ROLE)){
                        createdUser = userService.getSaftyUser(createdUser);
                    }
                    pictureVO.setCreatedUser(createdUser);
                    return pictureVO;
                })
                .collect(Collectors.toList());
        // 封装返回
        PicturePageVO picturePageVO = new PicturePageVO();
        picturePageVO.setPictureList(pictureVOList);
        picturePageVO.setTotal(picturePage.getTotal());
        return picturePageVO;
    }

    /**
     * 根据id获取图片
     */
    @Override
    public PictureVO getPictureById(Long id, User currentUser) {
        ThrowExceptionUtils.throwIF(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = this.getById(id);
        ThrowExceptionUtils.throwIF(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 判断图片是否在私人空间内
        if (picture.getSpaceId() != 0L) {
            Space space = spaceService.getById(picture.getSpaceId());
            // 判断当前用户是否有权限修改
            this.SpaceCheck(space.getUserId(), currentUser);
        }
        PictureVO pictureVO = getPictureVO(picture);
        // 不脱敏，直接设置完整的创建用户信息
        User createdUser = userService.getById(picture.getUserid());
        if (ObjectUtil.isNull(currentUser) || !currentUser.getUserstatus().equals(UserConstant.ADMIN_ROLE)){
            createdUser = userService.getSaftyUser(createdUser);
        }
        pictureVO.setCreatedUser(createdUser);
        return pictureVO;
    }

    /**
     * 修改图片（仅图片所属用户可修改）
     */
    @Override
    public Boolean editPicture(PictureUpdateRequest pictureUpdateRequest, User loginUser) {
        ThrowExceptionUtils.throwIF(pictureUpdateRequest == null || pictureUpdateRequest.getId() == null,
                ErrorCode.PARAMS_ERROR);
        ThrowExceptionUtils.throwIF(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        Picture picture = this.getById(pictureUpdateRequest.getId());
        ThrowExceptionUtils.throwIF(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 校验是否为图片所属用户
        ThrowExceptionUtils.throwIF(!picture.getUserid().equals(loginUser.getId()) && !loginUser.getUserstatus().equals("admin"),
                ErrorCode.NO_AUTH_ERROR, "无权限修改此图片");
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, updatePicture);
        String tagsStr = JSONUtil.toJsonStr(pictureUpdateRequest.getTags());
        System.out.println("转换后的tags："+tagsStr);
        updatePicture.setTags(tagsStr);
        updatePicture.setUpdatetime(new Date());
        boolean result = this.updateById(updatePicture);
        // 清除缓存，保证数据一致性
        if (result) {
            multiCacheManager.invalidatePicturePageCache();
        }
        return result;
    }

    /**
     * 获取单条图片包装类
     */
    @Override
    public PictureVO getPictureVO(Picture picture) {
        PictureVO pictureVO = PictureVO.obj2VO(picture);
        User crateUser = new User();
        /**
         * 封装脱敏后的创建用户信息
         */
        if (ObjectUtil.isNotNull(picture.getUserid()) && ObjectUtil.isNotEmpty(picture.getUserid())){
            crateUser = userService.getSaftyUser(userService.getById(picture.getUserid()));
        }
        pictureVO.setCreatedUser(crateUser);
        // 将tags从JSON字符串转为List<String>
        if (StrUtil.isNotBlank(picture.getTags())) {
            try {
                List<String> tagList = JSONUtil.toList(picture.getTags(), String.class);
                pictureVO.setTags(tagList);
            } catch (Exception e) {
                // JSON解析失败时，将原始字符串作为单个标签
                List<String> tagList = new ArrayList<>();
                tagList.add(picture.getTags());
                pictureVO.setTags(tagList);
            }
        }
        return pictureVO;
    }

    /**
     * 获取分页图片包装类
     * @param picturePage
     * @param request
     * @return
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictures = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getPages(),picturePage.getTotal());
        if (CollUtil.isEmpty(pictures)){
            return pictureVOPage;
        }

        // 封装对象VO列表
        List<PictureVO> pictureVOS = pictures.stream().map(picture -> {
            return PictureVO.obj2VO(picture);
        }).collect(Collectors.toList());
        // 将图片中的userid都取出来
        Set<Long> userIdSet = pictures.stream().map(Picture::getUserid).collect(Collectors.toSet());
        Map<Long,List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        pictureVOS.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if(userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setCreatedUser(user);
        });
        pictureVOPage.setRecords(pictureVOS);
        return pictureVOPage;
    }

    /**
     * 图片信息校验
     * @param picture
     */
    @Override
    public void vaildPicture(Picture picture) {
        ThrowExceptionUtils.throwIF(
                ObjectUtil.isEmpty(picture),
                ErrorCode.PARAMS_ERROR
        );
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 进行数据更新时，id不能为空，有参数则进行校验
        ThrowExceptionUtils.throwIF(
                ObjectUtil.isNull(id),
                ErrorCode.PARAMS_ERROR,
                "id不能为空"
        );
        if (StrUtil.isNotBlank(url)){
            ThrowExceptionUtils.throwIF(
                    url.length() > 1024,
                    ErrorCode.PARAMS_ERROR,
                    "url过长"
            );
        }
        if (StrUtil.isNotBlank(introduction)){
            ThrowExceptionUtils.throwIF(
                    introduction.length()>800,
                    ErrorCode.PARAMS_ERROR,
                    "简介过长"
            );
        }
    }

    /**
     * 管理员审核功能
     *
     * @param adminCheckPictureRequest
     * @param currentUser
     * @return
     */
    @Override
    public Boolean adminCheck(@RequestBody AdminCheckPictureRequest adminCheckPictureRequest, User currentUser) {
        Long picId = adminCheckPictureRequest.getPicId();
        Integer checkResult = adminCheckPictureRequest.getCheckResult();
        String checkMessage = adminCheckPictureRequest.getCheckMessage();
        ThrowExceptionUtils.throwIF(
                ObjectUtil.isNull(picId),
                ErrorCode.PARAMS_ERROR,
                "审核图片ID为空"
        );
        Picture oldPicture = this.getById(picId);
        ThrowExceptionUtils.throwIF(ObjectUtil.isNull(oldPicture),ErrorCode.PARAMS_ERROR,"待审核图片不存在");
        ThrowExceptionUtils.throwIF(oldPicture.getPictureCheck().equals(checkResult),ErrorCode.PARAMS_ERROR,"请勿重复审核");
        ThrowExceptionUtils.throwIF(
                ObjectUtil.isNull(checkResult),
                ErrorCode.PARAMS_ERROR,
                "审核状态异常"
        );
        if (Objects.equals(checkResult, PictureConstant.CHECK_REFUSE) && StrUtil.isBlank(checkMessage)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "审核人未提供审核拒绝原因");
        }
        if (checkResult.equals(PictureConstant.CHECK_PASS)) {
            checkMessage = "审核通过";
        }
        Picture picture = this.getById(adminCheckPictureRequest.getPicId());
        picture.setPictureCheck(checkResult);
        picture.setCheckMessage(checkMessage);
        picture.setCheckAdminId(currentUser.getId());
        picture.setCheckTime(DateTime.now());
        boolean result = this.updateById(picture);
        // 清除缓存，保证数据一致性
        if (result) {
            multiCacheManager.invalidatePicturePageCache();
        }
        return result;
    }

    /**
     * 管理员审核功能
     *
     * @param adminCheckPictureBatchRequest
     * @param currentUser
     * @return
     */
    @Override
    public Boolean adminCheckBatch(@RequestBody AdminCheckPictureBatchRequest adminCheckPictureBatchRequest, User currentUser) {
        List<Long> picIds = adminCheckPictureBatchRequest.getPicIds();
        Integer checkResult = adminCheckPictureBatchRequest.getCheckResult();
        String checkMessage = adminCheckPictureBatchRequest.getCheckMessage();
        //1. 如果没有审核拒绝时没有提交原因 直接throw
        if (Objects.equals(checkResult, PictureConstant.CHECK_REFUSE) && StrUtil.isBlank(checkMessage)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "审核人未提供审核拒绝原因");
        }
        ThrowExceptionUtils.throwIF(
                ObjectUtil.isNull(picIds),
                ErrorCode.PARAMS_ERROR,
                "审核图片Ids为空"
        );
        //1. 根据batch中的ids进行循环处理
        for(Long picId : picIds){
            //1.1 图片校验 = 是否存在 是否已审核 是否提交拒绝原因
            Picture oldPicture = this.getById(picId);
            ThrowExceptionUtils.throwIF(ObjectUtil.isNull(oldPicture), ErrorCode.PARAMS_ERROR, "待审核图片不存在");
            ThrowExceptionUtils.throwIF(oldPicture.getPictureCheck().equals(checkResult), ErrorCode.PARAMS_ERROR, "请勿重复审核");
            ThrowExceptionUtils.throwIF(
                    ObjectUtil.isNull(checkResult),
                    ErrorCode.PARAMS_ERROR,
                    "审核状态异常"
            );
            if (checkResult.equals(PictureConstant.CHECK_PASS)) {
                checkMessage = "审核通过";
            }
            //2. DB操作
            Picture picture = this.getById(picId);
            picture.setPictureCheck(checkResult);
            picture.setCheckMessage(checkMessage);
            picture.setCheckAdminId(currentUser.getId());
            picture.setCheckTime(DateTime.now());
            boolean result = this.updateById(picture);
            // 清除缓存，保证数据一致性
            if (result) {
                multiCacheManager.invalidatePicturePageCache();
            }
        }
        return true;
    }

    /**
     * 管理员批量拉取并上传图片
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return
     */
    @Override
    public Integer UploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        //1. 校验参数
        ThrowExceptionUtils.throwIF(loginUser == null,
                ErrorCode.NOT_LOGIN_ERROR);
        ThrowExceptionUtils.throwIF(pictureUploadByBatchRequest == null
                , ErrorCode.PARAMS_ERROR, "Batch请求体为空");
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowExceptionUtils.throwIF(StrUtil.isBlank(searchText),
                ErrorCode.PARAMS_ERROR,"搜索关键词为空");
        ThrowExceptionUtils.throwIF(count>20,ErrorCode.PARAMS_ERROR,"抓取上限为20条");
        //2. 抓取内容
        //2.1 构造抓取URL
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1",searchText);
        Document document;
        try {
            //2.2 利用Jsoup，先.connect链接对应的URL，再.get()拿到对应的Document
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败",e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"获取页面失败");
        }
        //3. 解析内容
        // Document:可以理解为全面的HTML，根据class类名，id获取到其中的内容；
        // 3.1. 找到所有图片容器 a.iusc
        Elements items = document.select("a.iusc");
        List<String> imgUrlList = new ArrayList<>();
        // 3.2 解析并验证地址
        for (Element item : items) {
            // 限定抓取张数
            if (imgUrlList.size() >= count) {
                break;
            }
            String mJson = item.attr("m");
            if (mJson.isEmpty()) continue;
            // Gson 解析
            Map<String, Object> map = GSON.fromJson(mJson, new TypeToken<Map<String, Object>>() {}.getType());
            String realUrl = (String) map.get("murl");
            if (realUrl != null && !realUrl.isBlank()) {
                imgUrlList.add(realUrl);
            }else {
                log.error("第"+(imgUrlList.size()+1)+"次图片拉取失败");
            }
        }
        //4. 校验抓取到的内容
        //5. 上传图片（复用已经写好的用管理员URL上传图片）
        Integer number = 0;
        PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
        String name = pictureUploadByBatchRequest.getName();
        if(StrUtil.isBlank(name)){
            name = searchText;
        }
        for (String inputSource : imgUrlList) {
            // 用关键词当做图片名称，防止图片名称为乱码 加上uuid 防止重名
            String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 4);// 长度：8
            pictureUploadRequest.setName(name+": "+uuid);
            pictureUploadRequest.setIntroduction(searchText+"相关图片");
            System.out.println("正在上传图片");
            // 调用URL上传接口 并自动过审
            try {
                PictureVO pictureVO = this.uploadPicture2DB(inputSource, pictureUploadRequest, loginUser);
                if (pictureVO != null) {
                    log.debug("图片上传成功，id= "+pictureVO.getId());
                    number++;
                }
            }catch (Exception e){
                log.error("图片上传失败",e);
            }
        }
        // 批量上传完成后，清除缓存保证数据一致性
        if (number > 0) {
            multiCacheManager.invalidatePicturePageCache();
        }
        return number;
    }

    /**
     * 构建分页查询条件
     * @param pictureQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        ThrowExceptionUtils.throwIF(
                pictureQueryRequest == null,
                ErrorCode.NOT_LOGIN_ERROR,
                "请求体为空"
        );
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        // 如果spaceId不为空的话，controller中已经确认了当前用户是空间持有人
        Long spaceId = pictureQueryRequest.getSpaceId();
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        String searchText = pictureQueryRequest.getSearchText();
        List<String> tags = pictureQueryRequest.getTags();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortFiled();
        String sortOrder = pictureQueryRequest.getSortOrder();
        // 审核通过状态
        Integer pictureCheck = pictureQueryRequest.getPictureCheck();
        // 如果没有限定审核状态，则默认查询通过的图片
        if (ObjectUtil.isNull(pictureCheck)) {
            pictureCheck = PictureConstant.CHECK_PASS;
        }
        /**
         * 用searchText从多字段中筛选
         */
        if (StrUtil.isNotBlank(searchText)){
            queryWrapper.and(qw -> qw.like("name",searchText)
                    .or()
                    .like("introduction",searchText));
        }
        if (spaceId < 0) {
            // 管理员查询所有私人空间的
            // 条件：spaceId 不为 null 且 不为 0
            queryWrapper.ne(ObjectUtil.isNotNull(spaceId), "spaceId", 0);
        } else {
            // 查询对应的库
            queryWrapper.eq(ObjectUtil.isNotNull(spaceId), "spaceId", spaceId);
        }
        queryWrapper.eq(ObjectUtil.isNotNull(id),"id",id);
        queryWrapper.eq(ObjectUtil.isNotNull(userId),"userId",userId);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        queryWrapper.lambda().eq(Picture::getPictureCheck, pictureCheck);

        // 接收到List<String>格式的tags，转化为对JSON的查询
        if (CollUtil.isNotEmpty(tags)){
            queryWrapper.and(pictureQueryWrapper -> {
                for (String tag : tags){
                    pictureQueryWrapper.apply("JSON_VALID(tags) AND JSON_CONTAINS(tags, JSON_QUOTE({0}))", tag);
                }
            });
        }
        return queryWrapper;
    }

    /**
     * 权限校验，仅管理员和图片所属者可以通过校验
     * @param loginUser
     * @param picture
     */
    private static void AuthCheck(User loginUser, Picture picture) {
        System.out.println("当前用户角色为："+loginUser.getUserstatus());
        ThrowExceptionUtils.throwIF(
                !loginUser.getUserstatus().equals(UserConstant.ADMIN_ROLE) && !loginUser.getId().equals(picture.getUserid()),
                ErrorCode.NO_AUTH_ERROR
        );
    }

    /**
     * 拉取公共图库的图片，到用户的个人空间
     * 免审核，免上传 => 仅DB操作copy即可
     */

    /**
     * 批量审核
     */
}
