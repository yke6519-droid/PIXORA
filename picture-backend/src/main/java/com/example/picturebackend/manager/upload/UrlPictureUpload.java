package com.example.picturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.example.picturebackend.Exception.BusinessException;
import com.example.picturebackend.Exception.ErrorCode;
import com.example.picturebackend.Exception.ThrowExceptionUtils;
import com.example.picturebackend.constant.PictureConstant;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Service
public class UrlPictureUpload extends PictureUploadTemplate {
    @Override
    protected void vailPic(Object inputSource) {
        String fileURL = (String) inputSource;
        // 校验是否为空
        ThrowExceptionUtils.throwIF(StrUtil.isBlank(fileURL), ErrorCode.PARAMS_ERROR,"文件URL为空！");
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
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg","image/png","image/webp");
                // 部分服务器会返回 image/jpeg;charset=UTF-8，只比较媒体类型主体。
                String mediaType = contentType.split(";")[0].trim().toLowerCase();
                ThrowExceptionUtils.throwIF(!ALLOW_CONTENT_TYPES.contains(mediaType)
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
        }catch (BusinessException e) {
            // 主动校验失败必须返回给前端，不能被网络异常兜底逻辑吞掉。
            throw e;
        }catch (RuntimeException e){
            // 网络请求失败，记录日志但不阻断流程
            // 可能是跨域、服务器不支持HEAD请求等情况
            System.out.println("URL图片校验失败，但继续上传: " + e.getMessage());
        } finally {
            if (httpResponse !=null){
                httpResponse.close();
            }
        }
    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        String fileURL = (String) inputSource;
        try {
            // 只取 URL 路径中的文件名，避免查询参数污染后缀判断。
            return FileUtil.getName(new URL(fileURL).getPath());
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "URL格式不正确");
        }
    }

    @Override
    protected void processFile(Object inputSource, File file) {
        String fileURL = (String) inputSource;
        HttpUtil.downloadFile(fileURL,file);
    }
}
