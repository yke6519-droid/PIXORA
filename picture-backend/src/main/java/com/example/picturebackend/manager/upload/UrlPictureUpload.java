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
                ThrowExceptionUtils.throwIF(!ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase())
                        ,ErrorCode.PARAMS_ERROR,"文件格式不正确！");
            }
            String contentLength = httpResponse.header("Content_Length");
            //文件大小不为空，进行校验
            try {
                if (StrUtil.isNotBlank(contentLength)){
                    long length = Long.parseLong(contentLength);
                    final long ONE_M = 1024*1024;
                    ThrowExceptionUtils.throwIF(length >2*ONE_M,
                            ErrorCode.PARAMS_ERROR,"文件大小不能超过2M");
                }
            }catch (NumberFormatException e){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"文件大小格式异常");
            }
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
        return FileUtil.mainName(fileURL);
    }

    @Override
    protected void processFile(Object inputSource, File file) {
        String fileURL = (String) inputSource;
        HttpUtil.downloadFile(fileURL,file);
    }
}
