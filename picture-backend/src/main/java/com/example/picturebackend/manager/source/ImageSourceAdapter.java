package com.example.picturebackend.manager.source;

import java.io.IOException;
import java.util.List;

/** 图片来源适配器：屏蔽不同图库的搜索协议，向批量导入提供统一候选。 */
public interface ImageSourceAdapter {
    /**
     * 根据关键词搜索图片，统一返回可以直接上传的图片地址。
     */
    List<String> search(String keyword, int candidateCount) throws IOException;

    /**
     * 带单次请求超时时间的搜索方法，旧适配器可以直接复用原方法。
     */
    default List<String> search(String keyword, int candidateCount, long timeoutMillis) throws IOException {
        return search(keyword, candidateCount);
    }
}
