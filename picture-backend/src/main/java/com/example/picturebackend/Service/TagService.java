package com.example.picturebackend.Service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.example.picturebackend.domain.po.Tag;
import com.example.picturebackend.domain.MyEnums.TagStatus;
import com.example.picturebackend.domain.po.User;

import java.util.List;

/**
* @author chen
* @description 针对表【tag(图片标签表)】的数据库操作Service
* @createDate 2026-04-28 18:35:09
*/
public interface TagService extends IService<Tag> {

    /** 查询空间标签；普通图片标签选择器只应传 false。 */
    List<Tag> listBySpaceId(Long spaceId, boolean includeDisabled, User loginUser);

    /** 创建空间标签，标签创建数量受空间等级限制。 */
    Tag createTag(Long spaceId, String tagName, User loginUser);

    /** 空间持有人改名，图片关联关系不变。 */
    Boolean renameTag(Long tagId, String tagName, User loginUser);

    /** 空间持有人删除标签实体，并清理全部图片关联。 */
    Boolean deleteTag(Long tagId, User loginUser);

    /** 管理员停用标签；历史关联保留。 */
    Boolean disableTag(Long tagId, User adminUser);

    /** 管理员恢复标签；恢复前校验单图标签上限。 */
    Boolean restoreTag(Long tagId, User adminUser);

    /** 内部统一的状态修改入口，供停用和恢复复用。 */
    Boolean updateStatus(Long tagId, TagStatus status, User adminUser);
}
