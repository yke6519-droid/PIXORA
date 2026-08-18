package com.example.picturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.example.picturebackend.Exception.ErrorCode;
import com.example.picturebackend.Exception.ThrowExceptionUtils;
import com.example.picturebackend.constant.PictureConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

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
        ThrowExceptionUtils.throwIF(
                fileSize > PictureConstant.MAX_PICTURE_SIZE_BYTES,
                ErrorCode.PARAMS_ERROR,
                "图片大小不能超过5MB"
        );
        String originalFilename = multipartFile.getOriginalFilename();
        ThrowExceptionUtils.throwIF(
                originalFilename == null || originalFilename.isBlank(),
                ErrorCode.PARAMS_ERROR,
                "文件名不能为空"
        );
        // 扩展名不区分大小写，避免 IMG_0132.JPG 这类合法图片被误判。
        String fileSuffix = FileUtil.getSuffix(originalFilename).toLowerCase(Locale.ROOT);
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
