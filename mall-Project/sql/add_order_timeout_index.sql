-- ============================================
-- 超时关单扫表所需索引
-- ============================================
-- 背景：超时关单由 MQ 延迟消息改为 OrderTimeoutTask 定时扫表驱动，
--       扫描条件是 status = 0 AND create_time < 截止时间 AND id > 游标。
-- 原有的 idx_status 与 idx_create_time 是两个独立单列索引，
-- 无法同时利用「status 等值 + create_time 范围」，优化器只能走其中一个，
-- 订单量增长后会退化为大量回表。
--
-- 本脚本可反复执行：索引已存在时跳过，不会报错。

SET @dbname = DATABASE();
SET @tablename = 'orders';
SET @indexname = 'idx_status_create';

SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (index_name = @indexname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD INDEX ', @indexname, ' (status, create_time)')
));
PREPARE addIndexIfNotExists FROM @preparedStatement;
EXECUTE addIndexIfNotExists;
DEALLOCATE PREPARE addIndexIfNotExists;

-- idx_status 是 (status, create_time) 的最左前缀，已被完全覆盖，可安全删除。
-- 保留会额外增加写入时的索引维护开销。
SET @indexname = 'idx_status';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (index_name = @indexname)
  ) > 0,
  CONCAT('ALTER TABLE ', @tablename, ' DROP INDEX ', @indexname),
  'SELECT 1'
));
PREPARE dropIndexIfExists FROM @preparedStatement;
EXECUTE dropIndexIfExists;
DEALLOCATE PREPARE dropIndexIfExists;

-- 验证：应能看到 idx_status_create，且不再有 idx_status
SHOW INDEX FROM orders WHERE Key_name IN ('idx_status', 'idx_status_create');

SELECT '✅ orders 表超时关单索引处理完成！' AS message;
