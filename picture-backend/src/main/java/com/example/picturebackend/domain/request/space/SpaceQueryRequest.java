package com.example.picturebackend.domain.request.space;

import com.example.picturebackend.domain.request.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
// 空间查询对象的相等性需要同时包含父类中的分页和排序字段。
@EqualsAndHashCode(callSuper = true)
public class SpaceQueryRequest extends PageRequest {
    /**
     * 空间id
     */
    private Long id;
    /**
     * 空间名
     */
    private String spaceName;
    /**
     * 空间等级
     */
    private Integer spaceLevel;
}
