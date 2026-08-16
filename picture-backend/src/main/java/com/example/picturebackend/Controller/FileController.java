package com.example.picturebackend.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.picturebackend.Config.CosClientConfig;
import com.example.picturebackend.Exception.ErrorCode;
import com.example.picturebackend.Exception.ThrowExceptionUtils;
import com.example.picturebackend.Service.AvatarCheckService;
import com.example.picturebackend.Service.UserService;
import com.example.picturebackend.Utils.ResponseUtils;
import com.example.picturebackend.annotation.AuthCheck;
import com.example.picturebackend.domain.po.AvatarCheck;
import com.example.picturebackend.domain.po.User;
import com.example.picturebackend.domain.request.BaseResponse;
import com.example.picturebackend.domain.vo.user.UploadAvatarVO;
import com.example.picturebackend.manager.CosManager;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import com.example.picturebackend.constant.PictureConstant;
import com.example.picturebackend.constant.UserConstant;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {
    @Resource
    private UserService userService;
    @Resource
    private CosManager cosManager;
    @Resource
    private CosClientConfig cosClientConfig;
    @Resource
    private AvatarCheckService avatarCheckService;

    /**
     * 接收并保存上传的头像 并返回图片url
     * @param file
     * @param request
     * @return 返回图片的url
     * @throws IOException
     */
    @PostMapping("/avatarUpload")
    // @AuthCheck(mustRole = "admin")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<UploadAvatarVO> avatarUpload(@RequestParam("avatar")MultipartFile file, HttpServletRequest request) throws IOException {
        // 判空处理
        ThrowExceptionUtils.throwIF(
                file == null || file.isEmpty(),
                ErrorCode.PARAMS_ERROR
        );

        ThrowExceptionUtils.throwIF(
            file.getOriginalFilename() == null || file.getOriginalFilename().isEmpty(),
            ErrorCode.PARAMS_ERROR,
            "文件名不能为空"
        );

        // 1. 获取原始文件名，仅用于校验文件后缀，不再直接作为存储文件名。
        String originalFileName = file.getOriginalFilename();

        // 判断格式是否合法
        String fileSuffix = FileUtil.getSuffix(originalFileName).toLowerCase(Locale.ROOT);
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "png", "jpg", "webp");
        ThrowExceptionUtils.throwIF(
                !ALLOW_FORMAT_LIST.contains(fileSuffix),
                ErrorCode.PARAMS_ERROR,
                "文件格式错误！"
        );

        // 判定文件大小是否超过5MB
        ThrowExceptionUtils.throwIF(
            file.getSize() > PictureConstant.MAX_PICTURE_SIZE_BYTES,
            ErrorCode.PARAMS_ERROR,
            "头像大小不得超过超过5MB"
        );

        // 2. 获取当前用户，并为头像生成独立且唯一的 COS 对象 Key。
        User user = userService.getCurrentUser(request);
        String objectKey = String.format(
                "public/avatar/%s/%s.%s",
                user.getId(),
                UUID.randomUUID(),
                fileSuffix
        );

        String host = cosClientConfig.getHost();
        String url = (host.endsWith("/") ? host : host + "/") + objectKey;
        File temporaryFile = null;
        boolean uploadAttempted = false;

        try {
            // COS 轮子目前接收 File，因此沿用已有 testUpload 的临时文件上传方式。
            temporaryFile = File.createTempFile("avatar_", "." + fileSuffix);
            file.transferTo(temporaryFile);
            uploadAttempted = true;
            cosManager.putObject(objectKey, temporaryFile);

            UploadAvatarVO uploadAvatarVO = new UploadAvatarVO();

            // 每次拿到该用户最新的一个
            AvatarCheck oldAvatarCheck = avatarCheckService.getOne(
                new QueryWrapper<AvatarCheck>()
                    .eq("userId", user.getId())
                    .orderByDesc("updateTime")
                    .last("LIMIT 1"));

            // 判断该用户目前是否已经有待审核头像
            Boolean hasCheck = false;
            // 若审核表中已经有了该记录，且是待审核状态 - 则后续进行更新，而不是新插入
            if (oldAvatarCheck != null && oldAvatarCheck.getStatus().equals(0)) {
                hasCheck = true;
            }

            // 审核头像
            if (user.getUserLevel().equals(UserConstant.ADMIN_ROLE)) {
                // 管理员自动审核通过
                AvatarCheck avatarCheck = new AvatarCheck();
                avatarCheck.setUrl(url);
                avatarCheck.setUserId(user.getId());
                avatarCheck.setStatus(1);
                avatarCheck.setCreatetime(DateTime.now());
                avatarCheck.setUpdatetime(DateTime.now());
                avatarCheck.setCheckMessage("管理员上传自动审核通过");

                // 将审核信息入库 avatarCheck
                boolean saved;
                if (hasCheck) {
                    avatarCheck.setId(oldAvatarCheck.getId());
                    saved = avatarCheckService.updateById(avatarCheck);
                } else {
                    saved = avatarCheckService.save(avatarCheck);
                }
                ThrowExceptionUtils.throwIF(!saved, ErrorCode.OPERATION_ERROR, "头像审核记录保存失败");

                // 管理员上传已经自动通过，当前头像必须在本次接口内完成更新，不能依赖资料接口补写。
                User avatarUser = new User();
                avatarUser.setId(user.getId());
                avatarUser.setAvatarurl(url);
                boolean userUpdated = userService.updateById(avatarUser);
                ThrowExceptionUtils.throwIF(!userUpdated, ErrorCode.OPERATION_ERROR, "当前头像更新失败");

                // 构造 VO
                uploadAvatarVO.setMessage("管理员上传自动审核通过");
                uploadAvatarVO.setStatus(1);
                uploadAvatarVO.setNewURL(url);
                return ResponseUtils.success(uploadAvatarVO);

            } else {
                // 普通用户先入库，状态为待审核，用户当前头像保持不变。
                AvatarCheck avatarCheck = new AvatarCheck();
                avatarCheck.setUrl(url);
                avatarCheck.setUserId(user.getId());
                avatarCheck.setStatus(0);
                avatarCheck.setCreatetime(DateTime.now());
                avatarCheck.setUpdatetime(DateTime.now());
                avatarCheck.setCheckMessage("待管理员审核");

                // 将审核信息入库 avatarCheck
                boolean saved;
                if (hasCheck) {
                    // 若为更新则输入旧记录的 id
                    avatarCheck.setId(oldAvatarCheck.getId());
                    saved = avatarCheckService.updateById(avatarCheck);
                } else {
                    saved = avatarCheckService.save(avatarCheck);
                }
                ThrowExceptionUtils.throwIF(!saved, ErrorCode.OPERATION_ERROR, "头像审核记录保存失败");
                return ResponseUtils.success(uploadAvatarVO);
            }
        } catch (IOException | RuntimeException e) {
            // COS 上传或审核记录落库失败时，尽量删除本次上传的 COS 对象，避免留下孤儿文件。
            if (uploadAttempted) {
                try {
                    cosManager.deleteObject(objectKey);
                } catch (RuntimeException cleanupException) {
                    log.error("头像 COS 对象清理失败, objectKey = {}", objectKey, cleanupException);
                }
            }
            throw e;
        } finally {
            if (temporaryFile != null && !temporaryFile.delete()) {
                log.error("头像临时文件删除失败, filePath = {}", temporaryFile.getAbsolutePath());
            }
        }
    }

    @PostMapping("testUpload")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<String> testUploadFile(@RequestParam("file")MultipartFile multipartFile){
        // 文件目录
        String filename = multipartFile.getOriginalFilename();
        // 返回给前端的路径
        String fileUrl = cosClientConfig.getHost()+"/" + filename;
        File file = null;
        try {
            // 上传文件
            file = File.createTempFile("upload_", null);
            multipartFile.transferTo(file);
            cosManager.putObject(filename, file);
            // 返回上传文件地址
            return ResponseUtils.success(fileUrl);
        }catch (IOException e){
            throw new RuntimeException(e);
        }finally {
            if (file!=null){
                //删除临时文件
                boolean delete = file.delete();
                if (!delete){
                    log.error("file delete error, filePath = {}",fileUrl);
                }
            }
        }
    }

    /**
     * 文件下载
     * @param request
     * @param response
     * @param filename
     * @throws IOException
     */
    @GetMapping("downloadFile")
    @AuthCheck(mustRole = "admin")
    public void testDownloadFile(HttpServletRequest request, HttpServletResponse response, String filename) throws IOException {
        COSObjectInputStream objectContent=null;
        try {
            COSObject cosObject = cosManager.getObject(filename);
            objectContent = cosObject.getObjectContent();
            byte[] bytes = IOUtils.toByteArray(objectContent);
            // 设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename="+filename);
            // 写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (objectContent != null) {
                objectContent.close();
            }
        }
    }
}
