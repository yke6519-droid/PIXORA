-- PIXORA 用户密码 BCrypt 升级。
--
-- 执行后，所有现有账号的原始密码会统一重置为：111111。
-- BCrypt 为不可逆哈希；该值由 BCrypt（强度 10）生成，应用登录时使用 matches 校验。
-- 本项目当前没有接入 Flyway/Liquibase，本文件由部署人员手工执行一次。

-- BCrypt 哈希长度为 60，预留 100 字符，且移除旧的明文默认密码。
ALTER TABLE `user`
    MODIFY COLUMN `userPassword` varchar(100) NOT NULL COMMENT '用户密码 BCrypt 哈希';

-- 所有已有账号统一重置密码。请在部署后通知用户修改初始密码。
UPDATE `user`
SET `userPassword` = '$2a$10$QMfP9ZdA4eombF2EdcdR4uHhw01R8PN2h8XDnpcTfUorooP4yJrEe',
    `updateTime` = CURRENT_TIMESTAMP;
