package com.example.picturebackend.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.picturebackend.domain.dto.file.UploadPictureResult;
import com.example.picturebackend.domain.po.Picture;
import com.example.picturebackend.domain.po.User;
import com.example.picturebackend.domain.request.picture.*;
import com.example.picturebackend.domain.vo.PicturePageVO;
import com.example.picturebackend.domain.vo.PictureVO;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author chen
* @description 针对表【picture(图库表)】的数据库操作Service
* @createDate 2026-04-28 18:35:01
*/
public interface PictureService extends IService<Picture> {

    Boolean SpaceCheck(Long userId, User loginUser);

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

    IPage<Picture> queryAll(PictureQueryRequest pictureQueryRequest, User adminUser);

    /**
     * 管理员分页获取图片列表
     */
    PicturePageVO queryPicturePage(PictureQueryRequest pictureQueryRequest, User currentUser);

    /**
     * 管理员根据id获取图片（不脱敏）
     */
    PictureVO getPictureById(Long id, User currentUser);

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
     * @param currentUser
     * @return
     */
    Boolean adminCheck(AdminCheckPictureRequest adminCheckPictureRequest, User currentUser);

    Boolean adminCheckBatch(@RequestBody AdminCheckPictureBatchRequest adminCheckPictureBatchRequest, User currentUser);

    /**
     * 按批次抓取并上传图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return
     */
    Integer UploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);
}
