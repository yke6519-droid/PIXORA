package com.example.picturebackend.manager;

import cn.hutool.core.date.DateUtil;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.example.picturebackend.Config.CosClientConfig;
import com.example.picturebackend.Exception.BusinessException;
import com.example.picturebackend.Exception.ErrorCode;
import com.example.picturebackend.Exception.ThrowExceptionUtils;
import com.example.picturebackend.constant.PictureConstant;

import com.example.picturebackend.domain.dto.file.UploadPictureResult;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.*;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 业务层面的
 * Cos对象的通用操作
 */
@Component
@Slf4j
public class FileManager {
    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片
     *
     * @param multipartFile 前端接收文件
     * @param uploadPrefix  上传路径前缀
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPrefix) {
        // 校验图片
        vailPic(multipartFile);
        // 图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
        // 自己拼接文件上传路径，而不是让用户自己上传（可能会导致url出异常）
        String fileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("/%s/%s", uploadPrefix, fileName);
        // 解析结果并返回
        File file = null;
        try {
            // 上传文件
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);
            // 获取上传对象结果
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 获取图片信息对象
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicwidth(imageInfo.getWidth());
            uploadPictureResult.setPicheight(imageInfo.getHeight());
            uploadPictureResult.setPicformat(imageInfo.getFormat());
            uploadPictureResult.setPicsize(FileUtil.size(file));
            uploadPictureResult.setPicscale(NumberUtil.round((double) imageInfo.getWidth() / imageInfo.getHeight(), 2).doubleValue());
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            return uploadPictureResult;
        } catch (IOException e) {
            log.error("图片上传到对象存储失败");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            deleteTemFile(file);
        }
    }

    /**
     * 校验图片
     *
     * @param multipartFile
     */
    private void vailPic(MultipartFile multipartFile) {
        ThrowExceptionUtils.throwIF(
                multipartFile == null,
                ErrorCode.PARAMS_ERROR,
                "上传的图片不能为空"
        );
        long fileSize = multipartFile.getSize();
        ThrowExceptionUtils.throwIF(
                fileSize > PictureConstant.MAX_PICTURE_SIZE_BYTES,
                ErrorCode.PARAMS_ERROR,
                "图片大小不能超过5MB"
        );
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "png", "jpg", "webp");
        ThrowExceptionUtils.throwIF(
                !ALLOW_FORMAT_LIST.contains(fileSuffix),
                ErrorCode.PARAMS_ERROR,
                "文件格式错误！"
        );
    }

    /**
     * 删除临时文件
     *
     * @param file
     */
    public static void deleteTemFile(File file) {
        if (file == null) {
            return;
        }
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filePath = {}", file.getAbsoluteFile());
        }
    }

    /**
     * 通过url地址 上传图片
     * @param fileURL 前端接收文件
     * @param uploadPrefix  上传路径前缀
     * @return
     */
    public UploadPictureResult uploadPictureByURL(String fileURL, String uploadPrefix) {
        // todo 校验图片的URL地址
        vailPicURL(fileURL);
        // 图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = FileUtil.mainName(fileURL);
        // 自己拼接文件上传路径，而不是让用户自己上传（可能会导致url出异常）
        String fileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("/%s/%s", uploadPrefix, fileName);
        // 解析结果并返回
        File file = null;
        try {
            // 上传文件
            file = File.createTempFile(uploadPath, null);
            //todo 利用Hutool工具类的HttpUtil.downloadFile，通过url下载文件
            // fileURL: 拿到的url下载地址 ； file：后端的文件路径
            HttpUtil.downloadFile(fileURL,file);
            // 获取上传对象结果
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 获取图片信息对象
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicwidth(imageInfo.getWidth());
            uploadPictureResult.setPicheight(imageInfo.getHeight());
            uploadPictureResult.setPicformat(imageInfo.getFormat());
            uploadPictureResult.setPicsize(FileUtil.size(file));
            uploadPictureResult.setPicscale(NumberUtil.round((double) imageInfo.getWidth() / imageInfo.getHeight(), 2).doubleValue());
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            return uploadPictureResult;
        } catch (IOException e) {
            log.error("图片上传到对象存储失败");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            deleteTemFile(file);
        }
    }

    /**
     * URL检验逻辑
     * 1. 是否空
     * 2. 格式正确？
     * 3. 协议正确？
     * 4. 从Header中拿到的 文件类型 与 文件大小 是否规范
     * @param fileURL
     */
    private void vailPicURL(String fileURL) {
        // 校验是否为空
        ThrowExceptionUtils.throwIF(StrUtil.isBlank(fileURL),ErrorCode.PARAMS_ERROR,"文件URL为空！");
        // 校验URL格式
        try{
            new URL(fileURL);
        }catch (MalformedURLException e){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"URL格式不正确");
        }
        // 校验URL的协议
        ThrowExceptionUtils.throwIF(!fileURL.startsWith("http://") && !fileURL.startsWith("https://"),
                ErrorCode.PARAMS_ERROR, "仅支持HTTP 或 HTTPS协议的文件地址");
        // 发送 HEAD请求 验证文件是否存在
        HttpResponse httpResponse = null;
        try {
            httpResponse = HttpUtil.createRequest(Method.HEAD, fileURL).execute();
            // 为正常返回则无需其他判断
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK){
                return; // 这里是为了兼容性，因此直接返回不报错；
            }
            // 若文件存在 校验文件格式与大小
            String contentType = httpResponse.header("Content-Type");
            // 文件格式不为空，再进行校验
            if (StrUtil.isNotBlank(contentType)){
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpag", "image/jpg","image/png","image/webp");
                ThrowExceptionUtils.throwIF(!ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase())
                        ,ErrorCode.PARAMS_ERROR,"文件格式不正确！");
            }
            String contentLength = httpResponse.header("Content-Length");
            //文件大小不为空，进行校验
            try {
                if (StrUtil.isNotBlank(contentLength)){
                    long length = Long.parseLong(contentLength);
                    ThrowExceptionUtils.throwIF(length > PictureConstant.MAX_PICTURE_SIZE_BYTES,
                            ErrorCode.PARAMS_ERROR,"文件大小不能超过5MB");
                }
            }catch (NumberFormatException e){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"文件大小格式异常");
            }
        }catch (RuntimeException e){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        } finally {
            if (httpResponse !=null){
                httpResponse.close();
            }
        }
    }
}
