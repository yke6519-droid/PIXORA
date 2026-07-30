package com.example.picturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;

import com.example.picturebackend.Config.CosClientConfig;
import com.example.picturebackend.Exception.BusinessException;
import com.example.picturebackend.Exception.ErrorCode;

import com.example.picturebackend.domain.dto.file.UploadPictureResult;
import com.example.picturebackend.manager.CosManager;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import lombok.extern.slf4j.Slf4j;


import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * 业务层面的
 * Cos对象的通用操作
 */
@Slf4j
public abstract class PictureUploadTemplate {
    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片
     *
     * @param inputSource 前端接收文件
     * @param uploadPrefix  上传路径前缀
     * @return
     */
    public UploadPictureResult uploadPicture(Object inputSource, String uploadPrefix) {
        // 1. 校验图片
        vailPic(inputSource);
        // 2. 图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = getOriginalFilename(inputSource);
        // 自己拼接文件上传路径，而不是让用户自己上传（可能会导致url出异常）
        String fileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("/%s/%s", uploadPrefix, fileName);
        File file = null;
        try {
            // 3.1 创建临时文件，获取图片到服务器
            file = File.createTempFile(uploadPath, null);
            // 3.2 处理文件来源
            processFile(inputSource,file);
            // 4. 上传对象存储，并获取上传对象结果
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 5. 获取图片信息对象
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 5.1 获取处理后的图片列表
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if (CollUtil.isNotEmpty(objectList)){
                // 获取压缩后的文件信息
                CIObject compressCiObject = objectList.get(0);
                CIObject thumbnailCiObject = objectList.get(1);
                // 封装压缩图的返回结果
                return getUploadPictureResult(originalFilename, compressCiObject, thumbnailCiObject);
            }
            // 6. 封装返回结果
            UploadPictureResult uploadPictureResult = getUploadPictureResult(originalFilename, imageInfo, file, uploadPath);
            return uploadPictureResult;
        } catch (IOException e) {
            log.error("图片上传到对象存储失败");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 删除临时文件
            deleteTemFile(file);
        }
    }
    protected abstract void vailPic(Object inputSource);

    protected abstract String getOriginalFilename(Object inputSource);

    protected abstract void processFile(Object inputSource,File file) throws IOException;

    /**
     * 封装压缩后的返回结果
     * @param originalFilename 原始文件名
     * @param compressCiObject 压缩后的对象
     * @return
     */
    private UploadPictureResult getUploadPictureResult(String originalFilename, CIObject compressCiObject, CIObject thumbnailCiObject) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
        uploadPictureResult.setPicwidth(compressCiObject.getWidth());
        uploadPictureResult.setPicheight(compressCiObject.getHeight());
        uploadPictureResult.setPicformat(compressCiObject.getFormat());
        uploadPictureResult.setPicsize(compressCiObject.getSize().longValue());
        uploadPictureResult.setPicscale(NumberUtil.round((double) compressCiObject.getWidth() / compressCiObject.getHeight(), 2).doubleValue());
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressCiObject.getKey());
        return uploadPictureResult;
    }

    /**
     * 封装返回结果
     * @param originalFilename
     * @param imageInfo 对象存储返回的解析信息
     * @param file
     * @param uploadPath
     * @return
     */
    private UploadPictureResult getUploadPictureResult(String originalFilename, ImageInfo imageInfo, File file, String uploadPath) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicwidth(imageInfo.getWidth());
        uploadPictureResult.setPicheight(imageInfo.getHeight());
        uploadPictureResult.setPicformat(imageInfo.getFormat());
        uploadPictureResult.setPicsize(FileUtil.size(file));
        uploadPictureResult.setPicscale(NumberUtil.round((double) imageInfo.getWidth() / imageInfo.getHeight(), 2).doubleValue());
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        return uploadPictureResult;
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
}
