package com.example.picturebackend.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.example.picturebackend.domain.dto.file.UploadPictureResult;
import com.example.picturebackend.domain.po.Picture;
import com.example.picturebackend.domain.po.User;
import com.example.picturebackend.domain.request.picture.*;
import com.example.picturebackend.domain.vo.picture.PictureListVO;
import com.example.picturebackend.domain.vo.picture.PicturePageVO;
import com.example.picturebackend.domain.vo.picture.PictureUploadVO;
import com.example.picturebackend.domain.vo.picture.PictureVO;

import org.springframework.web.bind.annotation.RequestBody;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
* @author chen
* @description 针对表【picture(图库表)】的数据库操作Service
* @createDate 2026-04-28 18:35:01
*/
public interface PictureService extends IService<Picture> {

    // Boolean AuthCheck(Long userId, User loginUser);

    /**
     * 通用上传图片到存储对象方法
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return 返回图片解析结果
     */
    public UploadPictureResult uploadPicture2COS(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * 上传图片 后端自动校验用户角色
     * 管理员 自动审核通过
     * 普通用户 待审核
     *
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    public PictureVO uploadPicture2DB(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * 批量上传本地图片；批量内部只在全部处理结束后统一清理一次分页缓存。
     */
    PictureUploadVO uploadPicture2DBBatch(List<MultipartFile> fileList,
                                          PictureUploadRequest pictureUploadRequest,
                                          User loginUser);

    /**
     * 管理员上传图片到数据库
     * 自动过审
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return PictureVO
     */

    /**
     * 管理员根据id删除图片
     */
    Boolean deletePictureById(Long id, User loginUser);

    /**
     * 管理员根据id更新图片信息
     */
    Boolean updatePictureById(PictureUpdateRequest pictureUpdateRequest, User loginUser);

    /**
     * 拿到所有图片的分页
     * @param pictureQueryRequest
     * @param adminUser
     * @return
     */
    IPage<PictureVO> queryAll(PictureQueryRequest pictureQueryRequest, User adminUser);

    /**
     * 管理员分页获取图片列表
     */
    PicturePageVO queryPicturePage(PictureQueryRequest pictureQueryRequest, User loginUser);

    /**
     * 使用多级缓存分页获取图片列表。
     *
     * 私有空间权限必须在读取缓存之前完成校验，缓存只能加速已经通过权限校验的查询，
     * 不能成为权限判断的替代品。
     */
    PicturePageVO queryPicturePageCache(PictureQueryRequest pictureQueryRequest, User loginUser);

    /**
     * 管理员根据id获取图片（不脱敏）
     */
    PictureVO getPictureById(Long id, User loginUser);

    /**
     * 修改图片（仅图片所属用户可修改）
     */
    Boolean editPicture(PictureUpdateRequest pictureUpdateRequest, User loginUser);

    /**
     * 根据请求体创建分页查询条件
     * @param pictureQueryRequest
     * @return
     */
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取单条图片包装类
     * @param picture
     * @return
     */
    public PictureVO getPictureVO(Picture picture);
    /**
     * 获取分页图片包装类
     * @param picturePage
     * @return
     */
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 校验图片
     * @param picture
     */
    public void vaildPicture(Picture picture);

    /**
     * 管理员校验 并更新图片状态
     *
     * @param adminCheckPictureRequest
     * @param loginUser
     * @return
     */
    Boolean adminCheck(AdminCheckPictureRequest adminCheckPictureRequest, User loginUser);

    Boolean adminCheckBatch(@RequestBody AdminCheckPictureBatchRequest adminCheckPictureBatchRequest, User loginUser);

    /**
     * 按批次抓取并上传图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return
     */
    PictureListVO UploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    /**
     * 重新上传图片
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO reloadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * 校验图片归属
     * @param loginUser
     * @param picture
     */
    void PictureAuthCheck(User loginUser, Picture picture);

    /**
     * 删除COS中的图片
     * @param picture
     */
    void deleteCosPicture(Picture picture);

    /**
     * 保存公共图库图片到指定私人空间
     * @return 
     */
    PictureVO save2Space(Save2SpaceRequest save2SpaceRequest, User loginUser);
}
