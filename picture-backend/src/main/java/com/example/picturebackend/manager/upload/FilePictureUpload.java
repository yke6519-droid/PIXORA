package com.example.picturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.example.picturebackend.Exception.ErrorCode;
import com.example.picturebackend.Exception.ThrowExceptionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class FilePictureUpload extends PictureUploadTemplate {
    @Override
    protected void vailPic(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        ThrowExceptionUtils.throwIF(
                multipartFile == null,
                ErrorCode.PARAMS_ERROR,
                "上传的图片不能为空"
        );
        long fileSize = multipartFile.getSize();
        final long ONE_M = 1024 * 1024;
        ThrowExceptionUtils.throwIF(
                fileSize > 2 * ONE_M,
                ErrorCode.PARAMS_ERROR,
                "图片大小不能超过2M"
        );
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "png", "jpg", "webp");
        ThrowExceptionUtils.throwIF(
                !ALLOW_FORMAT_LIST.contains(fileSuffix),
                ErrorCode.PARAMS_ERROR,
                "文件格式错误！"
        );
    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void processFile(Object inputSource, File file) throws IOException {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        multipartFile.transferTo(file);
    }
}
