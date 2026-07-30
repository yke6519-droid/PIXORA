package com.example.picturebackend.Controller;

import com.example.picturebackend.Config.CosClientConfig;
import com.example.picturebackend.Exception.ErrorCode;
import com.example.picturebackend.Exception.ThrowExceptionUtils;
import com.example.picturebackend.Service.UserService;
import com.example.picturebackend.Utils.ResponseUtils;
import com.example.picturebackend.annotation.AuthCheck;
import com.example.picturebackend.domain.po.User;
import com.example.picturebackend.domain.request.BaseResponse;
import com.example.picturebackend.manager.CosManager;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

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


    /**
     * 接收并保存上传的头像 并返回图片url
     * @param file
     * @param request
     * @return 返回图片的url
     * @throws IOException
     */
    @PostMapping("/avatarUpload")
    // @AuthCheck(mustRole = "admin")
    public BaseResponse<String> avatarUpload(@RequestParam("avatar")MultipartFile file, HttpServletRequest request) throws IOException {
        //1. 获取原始文件名
        String originalFileName = file.getOriginalFilename();

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
        return ResponseUtils.success(url);
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
