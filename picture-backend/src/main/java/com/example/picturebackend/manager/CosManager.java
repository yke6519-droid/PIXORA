package com.example.picturebackend.manager;

import cn.hutool.core.io.FileUtil;
import com.example.picturebackend.Config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Cos对象的通用操作
 */
@Component
public class CosManager {
    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     * @param key 唯一键：一个key对应一个文件
     * @param file 文件
     * @return
     */
    public PutObjectResult putObject(String key, File file){
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(),key,file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载对象
     * @param key
     * @return
     */
    public COSObject getObject(String key) throws IOException {
        GetObjectRequest objectRequest = new GetObjectRequest(cosClientConfig.getBucket(),key);
        COSObject cosObject = null;
        cosObject = cosClient.getObject(objectRequest);
        return cosObject;
    }

    /**
     * 上传并解析图片的方法
     * 新增：上传时对图片进行压缩处理
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        // 对图片进行处理
        PicOperations picOperations = new PicOperations();
        //1. 表示返回原图信息
        picOperations.setIsPicInfo(1);

        //2. 图片压缩处理
        //2.1 构造图片规则列表
        List<PicOperations.Rule> rules = new ArrayList<>();

        //2.2 构造压缩处理规则
        String webpKey = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setFileId(webpKey);
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setRule("imageMogr2/format/webp");
        rules.add(compressRule);

        //2.3 构造缩略图处理规则
        PicOperations.Rule thumbnailRule = new PicOperations.Rule();
        String thumbnailKey = FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key);
        thumbnailRule.setFileId(thumbnailKey);
        thumbnailRule.setBucket(cosClientConfig.getBucket());
        thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 512, 512));
        rules.add(thumbnailRule);

        //3. 构造处理参数
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 根据key 删除Cos中的对象
     * @param key
     */
    public void deleteObject(String key){
        try {
            System.out.println("要删除的key为:"+key);
            cosClient.deleteObject(cosClientConfig.getBucket(), key);
            System.out.println("已删除："+key);
        } catch (CosServiceException e) {
            e.printStackTrace();
        } catch (CosClientException e){
            e.printStackTrace();
        }
        
    }
    
    /**
     * 在当前存储桶中复制对象
     * @param sourceKey
     * @param targetKey
     */
    public void copyObject(String sourceKey, String targetKey){
        cosClient.copyObject(
            cosClientConfig.getBucket(),
            sourceKey,
            cosClientConfig.getBucket(),
            targetKey
        );
    }

    /**
     * 根据对象的key构造访问对象的url
     * @param key
     * @return
     */
    public String buildObjectUrl(String key){

        String host = cosClientConfig.getHost();

        if (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }

        if (key.startsWith("/")) {
            key = key.substring(1);
        }

        return host + "/" + key;
    }

}
