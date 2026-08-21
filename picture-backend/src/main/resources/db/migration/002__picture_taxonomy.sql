-- PIXORA 分类、空间标签及图片标签关系迁移。
--
-- 执行前提：001__baseline_core_schema.sql 已执行。
-- 本项目当前没有 Flyway/Liquibase，本文件由部署人员手工执行一次。
-- 不迁移旧 picture.category、picture.tags 的历史值；执行结束后旧字段会被删除。

-- 旧表没有业务数据，直接删除，避免旧命名与 picture_tag 产生歧义。
DROP TABLE IF EXISTS `picture-tag`;
DROP TABLE IF EXISTS `picture_tag`;

-- 旧 tag 表与新的空间标签模型不兼容，重建为新结构。
-- 当前版本不迁移旧 tag 数据。
DROP TABLE IF EXISTS `tag`;
CREATE TABLE `tag` (
    `id` bigint NOT NULL COMMENT '标签 id',
    `spaceId` bigint NOT NULL COMMENT '所属空间 id',
    `tagName` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '展示名称',
    `normalizedName` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规范化判重名称',
    `status` tinyint NOT NULL DEFAULT '1' COMMENT '标签状态，1-可用，0-停用',
    `createdBy` bigint NOT NULL COMMENT '创建用户 id',
    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tag_space_normalizedName` (`spaceId`, `normalizedName`),
    KEY `idx_tag_space_status` (`spaceId`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='空间标签表';

CREATE TABLE `category` (
    `id` bigint NOT NULL COMMENT '主题 id',
    `categoryName` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主题名称',
    `sortOrder` int NOT NULL DEFAULT '0' COMMENT '展示顺序',
    `isSystem` tinyint NOT NULL DEFAULT '0' COMMENT '是否系统主题，1-是，0-否',
    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_category_name` (`categoryName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公共图库主题表';

-- 固定 id=1 作为系统“未分类”主题，避免与雪花 id 冲突。
INSERT INTO `category` (`id`, `categoryName`, `sortOrder`, `isSystem`)
VALUES (1, '未分类', 0, 1);

CREATE TABLE `picture_tag` (
    `id` bigint NOT NULL COMMENT '关联记录 id',
    `pictureId` bigint NOT NULL COMMENT '图片 id',
    `tagId` bigint NOT NULL COMMENT '标签 id',
    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_picture_tag` (`pictureId`, `tagId`),
    KEY `idx_picture_tag_pictureId` (`pictureId`),
    KEY `idx_picture_tag_tagId` (`tagId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图片标签关联表';

-- 新主题字段先加入，再删除旧自由文本分类和 JSON 标签字段。
ALTER TABLE `picture`
    ADD COLUMN `categoryId` bigint NULL COMMENT '公共主题 id，个人空间图片允许为空' AFTER `category`,
    ADD KEY `idx_categoryId` (`categoryId`);

ALTER TABLE `picture`
    DROP INDEX `idx_category`,
    DROP INDEX `idx_tags`,
    DROP COLUMN `category`,
    DROP COLUMN `tags`;
