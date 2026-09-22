-- =============================================
-- 微信小程序登录支持：user 表新增 openid / unionid
-- 说明：
--   1. openid 是同一微信小程序内用户的唯一标识，登录时按 openid 匹配账号
--   2. unionid 在同一微信开放平台账号下的多个应用间一致，可选
--   3. MySQL 唯一索引允许多个 NULL，因此未绑定微信的历史账号不受影响
-- =============================================

ALTER TABLE `user`
    ADD COLUMN `openid`  VARCHAR(64) DEFAULT NULL COMMENT '微信 openid（小程序登录唯一标识）' AFTER `role`,
    ADD COLUMN `unionid` VARCHAR(64) DEFAULT NULL COMMENT '微信 unionid（开放平台统一标识）' AFTER `openid`;

ALTER TABLE `user`
    ADD UNIQUE KEY `uk_openid` (`openid`);

-- 校验
-- SHOW CREATE TABLE `user`;
