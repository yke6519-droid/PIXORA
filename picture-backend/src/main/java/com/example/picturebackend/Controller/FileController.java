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
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {
    private static final String file_upload_dir = "D:\\AAA项目学习\\spring项目\\智能云图库项目\\picture-backend\\avatarSave\\";
    @Resource
    private UserService userService;
    @Resource
    private Environment environment;
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

        //1. 获取原始文件名
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

        //2. 生成唯一文件名，放在在文件夹中重复 利用当前用户id作为分别点
        User user = userService.getCurrentUser(request);

        String newFileName= user.getId()+"-"+originalFileName;

        //3. 如果目录不存在，则创建目录
        File dir = new File(file_upload_dir);
        if (!dir.exists()){
            dir.mkdirs();
        }

        //4. 将图片保存下来
        file.transferTo(new File(file_upload_dir + newFileName));

        //5. 返回可以访问的URL地址
        String url = "http://localhost" +
                ":" + environment.getProperty("server.port") + //用environment动态获取服务器的端口号
                "/files/"+newFileName;

        UploadAvatarVO uploadAvatarVO = new UploadAvatarVO();

        // 每次拿到该用户最新的一个
        AvatarCheck oldAvatarCheck = avatarCheckService.getOne(
            new QueryWrapper<AvatarCheck>()
                .eq("userId", user.getId())
                .orderByDesc("updateTime")
                .last("LIMIT 1"));
        
        System.out.println("之前审核头像记录为："+oldAvatarCheck);
        
        // 判断该用户目前是否已经有待审核头像
        Boolean hasCheck = false;
        // 若审核表中已经有了该记录，且是待审核状态 - 则后续进行更新，而不是新插入

        if (oldAvatarCheck!=null && oldAvatarCheck.getStatus().equals(0)) {
            hasCheck = true;
        }

        System.out.println("当前有待审核的头像"+hasCheck);

        // 审核头像
        if (user.getUserLevel().equals(UserConstant.ADMIN_ROLE)) {
            // 管理员自动审核通过
            // 构造AvatarCheck
            AvatarCheck avatarCheck = new AvatarCheck();
            avatarCheck.setUrl(url);
            avatarCheck.setUserId(user.getId());
            avatarCheck.setStatus(1);
            avatarCheck.setCreatetime(DateTime.now());
            avatarCheck.setUpdatetime(DateTime.now());
            avatarCheck.setCheckMessage("管理员上传自动审核通过");

            // 将审核信息入库 avatarCheck
            if(hasCheck){
                avatarCheck.setId(oldAvatarCheck.getId());
                avatarCheckService.updateById(avatarCheck);
            }else{
                avatarCheckService.save(avatarCheck);
            }

            // 构造VO
            uploadAvatarVO.setMessage("管理员上传自动审核通过");
            uploadAvatarVO.setStatus(1);
            uploadAvatarVO.setNewURL(url);
            return ResponseUtils.success(uploadAvatarVO);
            
        } else{
            // 普通用户返回先入库正常入库，但是status为0
            AvatarCheck avatarCheck = new AvatarCheck();
            avatarCheck.setUrl(url);
            avatarCheck.setUserId(user.getId());
            avatarCheck.setStatus(0);
            avatarCheck.setCreatetime(DateTime.now());
            avatarCheck.setUpdatetime(DateTime.now());
            avatarCheck.setCheckMessage("待管理员审核");
            // 将审核信息入库 avatarCheck
            if(hasCheck){
                // 若为更新则输入旧记录的id
                avatarCheck.setId(oldAvatarCheck.getId());
                avatarCheckService.updateById(avatarCheck);
            }else{
                avatarCheckService.save(avatarCheck);
            }
            return ResponseUtils.success(uploadAvatarVO);
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
