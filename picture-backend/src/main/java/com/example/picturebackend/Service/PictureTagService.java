package com.example.picturebackend.Service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.example.picturebackend.domain.po.PictureTag;
import com.example.picturebackend.domain.po.User;

import java.util.List;

/** 图片与空间标签关系管理服务。 */
public interface PictureTagService extends IService<PictureTag> {

    /** 批量添加标签；任意一张图片超限时整批失败。 */
    Boolean addTags(Long spaceId, List<Long> pictureIds, List<Long> tagIds, User loginUser);

    /** 批量移除标签，只删除关联关系，不释放空间标签额度。 */
    Boolean removeTags(Long spaceId, List<Long> pictureIds, List<Long> tagIds, User loginUser);

    /** 恢复停用标签前校验历史关联是否会突破单图标签上限。 */
    void validateTagRestore(Long tagId);
}
