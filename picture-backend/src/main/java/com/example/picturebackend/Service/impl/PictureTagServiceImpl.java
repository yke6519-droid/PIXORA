package com.example.picturebackend.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.picturebackend.Exception.ErrorCode;
import com.example.picturebackend.Exception.ThrowExceptionUtils;
import com.example.picturebackend.Mapper.PictureMapper;
import com.example.picturebackend.Mapper.PictureTagMapper;
import com.example.picturebackend.Mapper.TagMapper;
import com.example.picturebackend.Service.PictureTagService;
import com.example.picturebackend.Service.SpaceService;
import com.example.picturebackend.domain.MyEnums.TagStatus;
import com.example.picturebackend.domain.po.Picture;
import com.example.picturebackend.domain.po.PictureTag;
import com.example.picturebackend.domain.po.Tag;
import com.example.picturebackend.domain.po.User;
import com.example.picturebackend.manager.MultiCacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 图片标签关联管理服务。
 *
 * <p>所有绑定规则集中在这里：空间一致性、标签状态、单图最多三个有效标签，
 * 以及批量操作的整批失败语义。</p>
 */
@Service
public class PictureTagServiceImpl extends ServiceImpl<PictureTagMapper, PictureTag>
        implements PictureTagService {

    private static final int MAX_ACTIVE_TAGS_PER_PICTURE = 3;

    @Resource
    private PictureMapper pictureMapper;

    @Resource
    private TagMapper tagMapper;

    @Resource
    private SpaceService spaceService;

    @Resource
    private MultiCacheManager multiCacheManager;

    //批量化增加照片
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean addTags(Long spaceId, List<Long> pictureIds, List<Long> tagIds, User loginUser) {
        validateSpaceAccess(spaceId, loginUser);

        List<Long> distinctPictureIds = distinctRequiredIds(pictureIds, "图片 id 不能为空");
        List<Long> distinctTagIds = distinctRequiredIds(tagIds, "标签 id 不能为空");

        // 锁定图片行，避免两个并发批量请求同时通过“单图最多 3 个标签”检查。
        loadPicturesInSpace(spaceId, distinctPictureIds, true);
        Map<Long, Tag> tagMap = loadTagsInSpace(spaceId, distinctTagIds, true);
        List<PictureTag> existingRelations = listRelationsByPictures(distinctPictureIds);

        // 额度统计需要读取图片已有的全部标签，而不是只读取本次请求的标签。
        Map<Long, Tag> existingTagMap = loadTagMap(existingRelations.stream()
                .map(PictureTag::getTagId)
                .collect(Collectors.toSet()));
        tagMap.putAll(existingTagMap);

        validateTagLimit(distinctPictureIds, distinctTagIds, existingRelations, tagMap);

        Set<String> existingKeys = existingRelations.stream()
                .map(relation -> relationKey(relation.getPictureId(), relation.getTagId()))
                .collect(Collectors.toSet());
        List<PictureTag> newRelations = new ArrayList<>();
        for (Long pictureId : distinctPictureIds) {
            for (Long tagId : distinctTagIds) {
                if (existingKeys.add(relationKey(pictureId, tagId))) {
                    PictureTag relation = new PictureTag();
                    relation.setPictureId(pictureId);
                    relation.setTagId(tagId);
                    relation.setCreateTime(new Date());
                    newRelations.add(relation);
                }
            }
        }

        if (!newRelations.isEmpty()) {
            ThrowExceptionUtils.throwIF(!this.saveBatch(newRelations),
                    ErrorCode.OPERATION_ERROR,
                    "图片标签绑定失败");
            multiCacheManager.invalidatePicturePageCache();
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean removeTags(Long spaceId, List<Long> pictureIds, List<Long> tagIds, User loginUser) {
        validateSpaceAccess(spaceId, loginUser);
        List<Long> distinctPictureIds = distinctRequiredIds(pictureIds, "图片 id 不能为空");
        List<Long> distinctTagIds = distinctRequiredIds(tagIds, "标签 id 不能为空");
        loadPicturesInSpace(spaceId, distinctPictureIds, false);
        loadTagsInSpace(spaceId, distinctTagIds, false);

        int deletedCount = this.getBaseMapper().delete(new QueryWrapper<PictureTag>()
                .in("pictureId", distinctPictureIds)
                .in("tagId", distinctTagIds));
        if (deletedCount > 0) {
            multiCacheManager.invalidatePicturePageCache();
        }
        return true;
    }

    @Override
    public void validateTagRestore(Long tagId) {
        ThrowExceptionUtils.throwIF(tagId == null, ErrorCode.PARAMS_ERROR, "标签 id 不能为空");
        Tag targetTag = tagMapper.selectById(tagId);
        ThrowExceptionUtils.throwIF(targetTag == null, ErrorCode.NOT_FOUND_ERROR, "标签不存在");
        // 校验当前标签状态是否已经是可用，若可用，则不操作
        if (TagStatus.ACTIVE.getValue().equals(targetTag.getStatus())) {
            return;
        }

        // 拿到该标签关联的 图片标签关联记录
        List<PictureTag> targetRelations = this.list(new QueryWrapper<PictureTag>().eq("tagId", tagId));
        if (targetRelations.isEmpty()) {
            return;
        }

        // 拿到关联图片的id
        List<Long> affectedPictureIds = targetRelations.stream()
                .map(PictureTag::getPictureId)
                .distinct()
                .toList();

        // 拿到关联的图片对象
        List<Picture> livePictures = pictureMapper.selectList(new QueryWrapper<Picture>()
                .in("id", affectedPictureIds)
                .eq("isDelete", 0)
                // 恢复标签与绑定标签都要串行操作同一批图片，避免恢复校验读到并发中间态。
                .last("FOR UPDATE"));

        if (livePictures.isEmpty()) {
            return;
        }

        // 拿到关联且未删除的图片id
        List<Long> livePictureIds = livePictures.stream().map(Picture::getId).toList();
        // 拿到存在的关联记录
        List<PictureTag> relations = listRelationsByPictures(livePictureIds);
        // 拿到所有的标签id
        Set<Long> allTagIds = relations.stream().map(PictureTag::getTagId).collect(Collectors.toSet());
        Map<Long, Tag> tagMap = loadTagMap(allTagIds);

        Map<Long, Integer> activeCountByPicture = new HashMap<>();
        for (PictureTag relation : relations) {
            Tag tag = tagMap.get(relation.getTagId());
            boolean activeAfterRestore = relation.getTagId().equals(tagId)
                    || (tag != null && TagStatus.ACTIVE.getValue().equals(tag.getStatus()));
            if (activeAfterRestore) {
                activeCountByPicture.merge(relation.getPictureId(), 1, Integer::sum);
            }
        }

        activeCountByPicture.forEach((pictureId, activeCount) ->
                ThrowExceptionUtils.throwIF(
                        activeCount > MAX_ACTIVE_TAGS_PER_PICTURE,
                        ErrorCode.PARAMS_ERROR,
                        "标签恢复后图片 " + pictureId + " 的有效标签将超过 3 个，请先解除冲突关联"));
    }

    private void validateTagLimit(List<Long> pictureIds,
                                  List<Long> tagIds,
                                  List<PictureTag> existingRelations,
                                  Map<Long, Tag> tagMap) {
        Map<Long, Set<Long>> existingTagIdsByPicture = new HashMap<>();
        Map<Long, Integer> activeCountByPicture = new HashMap<>();
        for (PictureTag relation : existingRelations) {
            existingTagIdsByPicture
                    .computeIfAbsent(relation.getPictureId(), ignored -> new HashSet<>())
                    .add(relation.getTagId());
            Tag existingTag = tagMap.get(relation.getTagId());
            if (existingTag != null && TagStatus.ACTIVE.getValue().equals(existingTag.getStatus())) {
                activeCountByPicture.merge(relation.getPictureId(), 1, Integer::sum);
            }
        }

        for (Long pictureId : pictureIds) {
            Set<Long> existingTagIds = existingTagIdsByPicture.getOrDefault(pictureId, Collections.emptySet());
            long newActiveTagCount = tagIds.stream()
                    .filter(tagId -> !existingTagIds.contains(tagId))
                    .count();
            int currentActiveTagCount = activeCountByPicture.getOrDefault(pictureId, 0);
            ThrowExceptionUtils.throwIF(
                    currentActiveTagCount + newActiveTagCount > MAX_ACTIVE_TAGS_PER_PICTURE,
                    ErrorCode.PARAMS_ERROR,
                    "图片 " + pictureId + " 最多绑定 3 个有效标签");
        }
    }

    // 通过图片id拿到关联表记录
    private List<PictureTag> listRelationsByPictures(List<Long> pictureIds) {
        if (pictureIds.isEmpty()) {
            return Collections.emptyList();
        }
        return this.list(new QueryWrapper<PictureTag>().in("pictureId", pictureIds));
    }

    private List<Picture> loadPicturesInSpace(Long spaceId, List<Long> pictureIds, boolean lockRows) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<Picture>()
                .in("id", pictureIds)
                .eq("spaceId", spaceId)
                .eq("isDelete", 0);
        if (lockRows) {
            queryWrapper.last("FOR UPDATE");
        }
        List<Picture> pictures = pictureMapper.selectList(queryWrapper);
        ThrowExceptionUtils.throwIF(
                pictures.size() != pictureIds.size(),
                ErrorCode.NO_AUTH_ERROR,
                "存在不属于当前空间的图片");
        return pictures;
    }

    private Map<Long, Tag> loadTagsInSpace(Long spaceId, List<Long> tagIds, boolean requireActive) {
        Map<Long, Tag> tagMap = loadTagMap(tagIds);
        ThrowExceptionUtils.throwIF(
                tagMap.size() != tagIds.size(),
                ErrorCode.NOT_FOUND_ERROR,
                "存在不存在的标签");
        for (Long tagId : tagIds) {
            Tag tag = tagMap.get(tagId);
            ThrowExceptionUtils.throwIF(
                    !spaceId.equals(tag.getSpaceId()),
                    ErrorCode.NO_AUTH_ERROR,
                    "标签不属于当前空间");
            if (requireActive) {
                ThrowExceptionUtils.throwIF(
                        !TagStatus.ACTIVE.getValue().equals(tag.getStatus()),
                        ErrorCode.PARAMS_ERROR,
                        "停用标签不能绑定新图片");
            }
        }
        return tagMap;
    }

    // 将TagList转为Map
    private Map<Long, Tag> loadTagMap(Iterable<Long> tagIds) {
        List<Long> idList = new ArrayList<>();
        tagIds.forEach(idList::add);
        
        if (idList.isEmpty()) {
            return Collections.emptyMap();
        }
        return tagMapper.selectBatchIds(idList).stream()
                .collect(Collectors.toMap(Tag::getId, tag -> tag));
    }

    private List<Long> distinctRequiredIds(List<Long> ids, String message) {
        ThrowExceptionUtils.throwIF(ids == null || ids.isEmpty(), ErrorCode.PARAMS_ERROR, message);

        LinkedHashSet<Long> distinctIds = ids.stream()
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
                
        ThrowExceptionUtils.throwIF(distinctIds.isEmpty(), ErrorCode.PARAMS_ERROR, message);
        return new ArrayList<>(distinctIds);
    }

    private void validateSpaceAccess(Long spaceId, User loginUser) {
        ThrowExceptionUtils.throwIF(spaceId == null || spaceId <= 0,
                ErrorCode.PARAMS_ERROR,
                "图片标签必须属于个人空间");
        ThrowExceptionUtils.throwIF(loginUser == null || loginUser.getId() == null,
                ErrorCode.NOT_LOGIN_ERROR,
                "请先登录");
        spaceService.SpaceAuthCheck(spaceId, loginUser);
    }

    private String relationKey(Long pictureId, Long tagId) {
        return pictureId + ":" + tagId;
    }
}
