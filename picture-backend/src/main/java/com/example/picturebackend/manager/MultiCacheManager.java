package com.example.picturebackend.manager;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.example.picturebackend.domain.po.User;
import com.example.picturebackend.domain.request.picture.PictureQueryRequest;
import com.example.picturebackend.domain.vo.PicturePageVO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * 多级缓存工具类
 * 实现三级缓存架构：本地缓存(Caffeine) -> 分布式缓存(Redis) -> 数据库
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MultiCacheManager {

    // 本地缓存 Caffeine
    private static final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024)
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(1))
            .build();

    // 缓存Key前缀
    private static final String CACHE_KEY_PREFIX = "picture:query:";

    // Spring 自动注入
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 多级缓存：本地缓存 → Redis → DB
     * @param queryRequest 查询请求
     * @param loginUser 当前登录用户
     * @param queryFunction 查询数据库的函数（避免循环依赖）
     * @return 分页结果
     */
    public PicturePageVO getPicturePage(PictureQueryRequest queryRequest, User loginUser,
                                        BiFunction<PictureQueryRequest, User, PicturePageVO> queryFunction) {
        // 1. 构造缓存 Key
        String jsonQuery = JSONUtil.toJsonStr(queryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(jsonQuery.getBytes());
        String cacheKey = CACHE_KEY_PREFIX + hashKey;

        // 2. 查本地缓存
        String localCache = LOCAL_CACHE.getIfPresent(cacheKey);
        if (StrUtil.isNotBlank(localCache)) {
            System.out.println("本地缓存命中，key: " + cacheKey);
            return JSONUtil.toBean(localCache, PicturePageVO.class);
        }

        // 3. 查 Redis
        String redisCache = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(redisCache)) {
            System.out.println("Redis缓存命中，key: " + cacheKey);
            LOCAL_CACHE.put(cacheKey, redisCache);
            return JSONUtil.toBean(redisCache, PicturePageVO.class);
        }

        // 4. 查数据库（通过传入的函数）
        System.out.println("缓存未命中，查询数据库");
        PicturePageVO result = queryFunction.apply(queryRequest, loginUser);
        String resultJson = JSONUtil.toJsonStr(result);

        // 5. 写入缓存
        LOCAL_CACHE.put(cacheKey, resultJson);
        int expire = 10 + RandomUtil.randomInt(30);
        stringRedisTemplate.opsForValue().set(cacheKey, resultJson, expire, TimeUnit.SECONDS);
        log.debug("数据已写入缓存，key: {}，过期时间: {}秒", cacheKey, expire);

        return result;
    }

    /**
     * 清除所有图片分页缓存
     * 在写操作（上传、更新、删除、审核）后调用，保证数据一致性
     * 采用Cache Aside模式：先更新DB，再清除缓存
     */
    public void invalidatePicturePageCache() {
        try {
            // 1. 清除Redis缓存 - 删除所有以 picture:query: 开头的key
            Set<String> keys = stringRedisTemplate.keys(CACHE_KEY_PREFIX + "*");
            if (CollUtil.isNotEmpty(keys)) {
                Long deletedCount = stringRedisTemplate.delete(keys);
                log.info("清除Redis分页缓存，数量: {}", deletedCount);
            } else {
                log.debug("Redis中没有分页缓存需要清除");
            }

            // 2. 清除本地缓存
            LOCAL_CACHE.invalidateAll();
            log.info("本地分页缓存已清除");
        } catch (Exception e) {
            log.error("清除缓存失败", e);
            // 缓存清除失败不影响主业务流程，只记录日志
        }
    }

    /**
     * 清除指定图片相关的缓存（预留方法，用于后续精准清除优化）
     * 目前实现为全量清除，后续可优化为只清除包含该图片的缓存页
     *
     * @param pictureId 图片ID
     */
    public void invalidatePictureCache(Long pictureId) {
        log.info("清除图片[{}]相关的缓存", pictureId);
        // 目前实现为全量清除，保证数据一致性
        invalidatePicturePageCache();
    }
}