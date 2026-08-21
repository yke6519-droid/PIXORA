-- PIXORA 基础数据库结构。
--
-- 本文件用于新环境初始化，按当前项目的基础表结构建立数据库。
-- picture.category 和 picture.tags 仅作为 002 迁移的过渡字段，
-- 不代表最终业务模型；002 执行完成后会删除这两个字段。
--
-- 当前项目没有接入 Flyway/Liquibase，本文件由部署人员手工执行一次。

CREATE TABLE IF NOT EXISTS `user` (
    `username` varchar(256) NOT NULL COMMENT '用户昵称',
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    `userAccount` varchar(256) NOT NULL COMMENT '用户账号',
    `avatarUrl` varchar(1024) DEFAULT NULL COMMENT '头像',
    `gender` tinyint DEFAULT NULL COMMENT '性别',
    `userPassword` varchar(50) NOT NULL DEFAULT '123456' COMMENT '用户密码',
    `phone` varchar(128) NOT NULL COMMENT '电话号码',
    `userLevel` varchar(64) NOT NULL DEFAULT 'user' COMMENT '用户级别：user-普通用户，admin-管理员，vip-VIP',
    `userStatus` tinyint NOT NULL DEFAULT '0' COMMENT '用户状态，0-正常，1-封禁',
    `email` varchar(512) DEFAULT NULL COMMENT '邮箱地址',
    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
    `profile` varchar(512) DEFAULT NULL COMMENT '个人简介',
    `spaceId` bigint DEFAULT NULL COMMENT '当前空间 id',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `space` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '空间 id',
    `spaceName` varchar(128) NOT NULL COMMENT '空间名称',
    `spaceLevel` int DEFAULT NULL COMMENT '空间级别：0-普通版，1-专业版，2-旗舰版',
    `maxSize` bigint NOT NULL COMMENT '空间最大容量',
    `usedSize` bigint NOT NULL DEFAULT '0' COMMENT '已经占用的空间大小',
    `maxCount` bigint NOT NULL COMMENT '空间图片数量上限',
    `usedCount` bigint NOT NULL DEFAULT '0' COMMENT '已存储图片数',
    `userId` bigint NOT NULL COMMENT '创建者 id',
    `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除，1 表示删除',
    PRIMARY KEY (`id`),
    KEY `idx_spaceLevel` (`spaceLevel`) COMMENT '按空间级别查询',
    KEY `idx_spaceName` (`spaceName`) COMMENT '按空间名称查询',
    KEY `idx_userId` (`userId`) COMMENT '按用户查询空间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户私人空间表';

CREATE TABLE IF NOT EXISTS `picture` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    `url` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图片 url',
    `name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图片名称',
    `introduction` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '简介',
    `category` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '旧自由文本分类，仅供 002 迁移删除',
    `tags` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '旧 JSON 标签，仅供 002 迁移删除',
    `picSize` bigint DEFAULT NULL COMMENT '图片大小',
    `picWidth` int DEFAULT NULL COMMENT '图片宽度',
    `picHeight` int DEFAULT NULL COMMENT '图片高度',
    `picScale` double DEFAULT NULL COMMENT '图片宽高比',
    `picFormat` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图片格式',
    `userId` bigint NOT NULL COMMENT '创建用户 id',
    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '编辑时间',
    `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除，0-未删除，1-已删除',
    `pictureCheck` tinyint NOT NULL DEFAULT '0' COMMENT '审核状态，0-待审核，1-通过，2-拒绝',
    `checkAdminId` bigint NOT NULL DEFAULT '0' COMMENT '审核管理员 id',
    `checkTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
    `checkMessage` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审核原因',
    `thumbnailUrl` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '缩略图 url',
    `spaceId` tinyint NOT NULL DEFAULT '0' COMMENT '绑定空间 id，0-公共图库',
    `pictureKey` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'COS 原图 key',
    `thumbnailKey` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'COS 缩略图 key',
    `originalKey` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'COS 原始文件 key',
    `sourcePictureId` bigint DEFAULT NULL COMMENT '图片来源 id，用于保存到个人空间',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category`) COMMENT '旧分类查询索引，002 中删除',
    KEY `idx_introduction` (`introduction`) COMMENT '简介模糊查询索引',
    KEY `idx_name` (`name`) COMMENT '图片名称查询索引',
    KEY `idx_tags` (`tags`) COMMENT '旧标签查询索引，002 中删除',
    KEY `idx_userId` (`userId`) COMMENT '创建用户查询索引',
    KEY `idx_checkAdminId` (`checkAdminId`) COMMENT '审核管理员查询索引',
    KEY `idx_pictureCheck` (`pictureCheck`) COMMENT '审核状态查询索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图片表';

CREATE TABLE IF NOT EXISTS `avatar_check` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    `url` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '头像 url',
    `userId` bigint NOT NULL COMMENT '申请用户 id',
    `status` tinyint NOT NULL DEFAULT '0' COMMENT '审核状态，0-待审核，1-通过，2-拒绝',
    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `checkMessage` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审核原因',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='头像审核记录表';

CREATE TABLE IF NOT EXISTS `user_notification` (
    `id` bigint NOT NULL COMMENT '通知 id，使用项目雪花算法生成',
    `userId` bigint NOT NULL COMMENT '接收通知的用户 id',
    `type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知类型',
    `title` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知标题',
    `content` varchar(1024) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知内容',
    `bizType` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务类型',
    `bizId` bigint NOT NULL COMMENT '关联业务记录 id',
    `readTime` datetime DEFAULT NULL COMMENT '已读时间，NULL 表示未读',
    `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除，0-未删除，1-已删除',
    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_type_biz` (`userId`, `type`, `bizType`, `bizId`) COMMENT '同一用户同一业务记录只能有一条通知',
    KEY `idx_user_delete_create` (`userId`, `isDelete`, `createTime`) COMMENT '通知列表查询索引',
    KEY `idx_user_delete_read` (`userId`, `isDelete`, `readTime`) COMMENT '未读通知查询索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户通知表';
