# 网购商城数据库设计文档

> **项目名称：** 网购商城系统  
> **作者：** 明耀  
> **创建时间：** 2026-05-09  
> **更新时间：** 2026-05-10  
> **数据库版本：** MySQL 8.0+

---

## 📊 数据库表概览

| 序号 | 表名 | 表说明 | 主要功能 |
|------|------|--------|----------|
| 1 | user | 用户表 | 存储用户基本信息 |
| 2 | category | 商品分类表 | 商品分类管理，支持多级分类 |
| 3 | product | 商品表 | 商品信息管理 |
| 4 | brand | 品牌表 | 品牌信息管理 |
| 5 | product_image | 商品图片表 | 商品多图片管理 |
| 6 | product_sku | 商品SKU规格表 | 商品规格库存管理 |
| 7 | product_param | 商品参数表 | 商品详细参数 |
| 8 | product_comment | 商品评价表 | 用户评价管理 |
| 9 | cart | 购物车表 | 用户购物车数据 |
| 10 | orders | 订单表 | 订单基本信息 |
| 11 | order_item | 订单详情表 | 订单商品明细 |

---

## 📋 表结构详细设计

### 1. 用户表

**表说明：** 存储用户的基本信息，支持用户名和手机号登录

| 字段名 | 类型 | 是否必填 | 默认值 | 说明 |
|--------|------|----------|--------|------|
| id | BIGINT | 是 | 自增 | 用户唯一标识 |
| username | VARCHAR(50) | 是 | - | 用户名（唯一） |
| password | VARCHAR(100) | 是 | - | 密码（BCrypt加密） |
| phone | VARCHAR(11) | 否 | NULL | 手机号（唯一） |
| email | VARCHAR(100) | 否 | NULL | 邮箱 |
| gender | TINYINT | 否 | 0 | 性别：0-未知，1-男，2-女 |
| avatar | VARCHAR(255) | 否 | NULL | 头像URL |
| status | TINYINT | 否 | 1 | 状态：0-禁用，1-正常 |
| create_time | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 否 | CURRENT_TIMESTAMP | 更新时间 |
| is_deleted | TINYINT | 否 | 0 | 逻辑删除：0-未删除，1-已删除 |

**索引设计：**
- 主键索引：`id`
- 唯一索引：`username`、`phone`
- 普通索引：`status`

---

### 2. 商品分类表

**表说明：** 商品分类管理，支持三级分类结构

| 字段名 | 类型 | 是否必填 | 默认值 | 说明 |
|--------|------|----------|--------|------|
| id | BIGINT | 是 | 自增 | 分类唯一标识 |
| name | VARCHAR(50) | 是 | - | 分类名称 |
| parent_id | BIGINT | 否 | 0 | 父分类ID（0表示一级分类） |
| level | TINYINT | 否 | 1 | 分类层级：1-一级，2-二级，3-三级 |
| sort | INT | 否 | 0 | 排序（数字越小越靠前） |
| icon | VARCHAR(255) | 否 | NULL | 分类图标URL |
| status | TINYINT | 否 | 1 | 状态：0-禁用，1-正常 |
| create_time | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 否 | CURRENT_TIMESTAMP | 更新时间 |
| is_deleted | TINYINT | 否 | 0 | 逻辑删除：0-未删除，1-已删除 |

| **分类结构示例：**

---

### 3. 商品表

**表说明：** 存储商品的基本信息、价格、库存等

| 字段名 | 类型 | 是否必填 | 默认值 | 说明 |
|--------|------|----------|--------|------|
| id | BIGINT | 是 | 自增 | 商品唯一标识 |
| name | VARCHAR(200) | 是 | - | 商品名称 |
| category_id | BIGINT | 是 | - | 分类ID（外键） |
| brand | VARCHAR(100) | 否 | NULL | 品牌 |
| main_image | VARCHAR(255) | 否 | NULL | 主图URL |
| sub_images | TEXT | 否 | NULL | 副图URL（多个用逗号分隔） |
| detail | TEXT | 否 | NULL | 商品详情（富文本） |
| price | DECIMAL(10,2) | 是 | - | 价格（元） |
| original_price | DECIMAL(10,2) | 否 | NULL | 原价（元） |
| stock | INT | 是 | 0 | 库存数量 |
| sales | INT | 否 | 0 | 销量 |
| status | TINYINT | 否 | 1 | 状态：0-下架，1-上架，2-售罄 |
| sort | INT | 否 | 0 | 排序（数字越小越靠前） |
| create_time | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 否 | CURRENT_TIMESTAMP | 更新时间 |
| is_deleted | TINYINT | 否 | 0 | 逻辑删除：0-未删除，1-已删除 |

**索引设计：**
- 主键索引：`id`
- 普通索引：`category_id`、`brand`、`price`、`sales`、`status`

---

### 10. 订单表

**表说明：** 记录订单的基本信息、状态流转、收货信息等

| 字段名 | 类型 | 是否必填 | 默认值 | 说明 |
|--------|------|----------|--------|------|
| id | BIGINT | 是 | 自增 | 订单唯一标识 |
| order_no | VARCHAR(64) | 是 | - | 订单号（唯一） |
| user_id | BIGINT | 是 | - | 用户ID（外键） |
| total_amount | DECIMAL(10,2) | 是 | - | 订单总金额 |
| pay_amount | DECIMAL(10,2) | 否 | NULL | 实付金额 |
| freight_amount | DECIMAL(10,2) | 否 | 0.00 | 运费 |
| discount_amount | DECIMAL(10,2) | 否 | 0.00 | 优惠金额 |
| payment_type | TINYINT | 否 | 1 | 支付方式：1-微信，2-支付宝，3-银行卡 |
| status | TINYINT | 否 | 0 | 订单状态：0-待支付，1-已支付，2-已发货，3-已完成，4-已取消，5-已退款 |
| payment_time | DATETIME | 否 | NULL | 支付时间 |
| delivery_time | DATETIME | 否 | NULL | 发货时间 |
| receive_time | DATETIME | 否 | NULL | 收货时间 |
| receiver_name | VARCHAR(50) | 是 | - | 收货人姓名 |
| receiver_phone | VARCHAR(11) | 是 | - | 收货人电话 |
| receiver_address | VARCHAR(255) | 是 | - | 收货地址 |
| note | VARCHAR(500) | 否 | NULL | 订单备注 |
| create_time | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 否 | CURRENT_TIMESTAMP | 更新时间 |
| is_deleted | TINYINT | 否 | 0 | 逻辑删除：0-未删除，1-已删除 |

---

### 4. 品牌表

**表说明：** 存储品牌基本信息，与商品表关联

| 字段名 | 类型 | 是否必填 | 默认值 | 说明 |
|--------|------|----------|--------|------|
| id | BIGINT | 是 | 自增 | 品牌唯一标识 |
| name | VARCHAR(100) | 是 | - | 品牌名称（唯一） |
| logo | VARCHAR(255) | 否 | NULL | 品牌Logo URL |
| description | VARCHAR(500) | 否 | NULL | 品牌描述 |
| sort | INT | 否 | 0 | 排序（数字越小越靠前） |
| status | TINYINT | 否 | 1 | 状态：0-禁用，1-正常 |
| create_time | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 否 | CURRENT_TIMESTAMP | 更新时间 |
| is_deleted | TINYINT | 否 | 0 | 逻辑删除：0-未删除，1-已删除 |

**索引设计：**
- 主键索引：`id`
- 唯一索引：`name`
- 普通索引：`status`、`sort`

---

### 5. 商品图片表 (product_image)

**表说明：** 存储商品多张图片，区分主图、详情图、规格图

| 字段名 | 类型 | 是否必填 | 默认值 | 说明 |
|--------|------|----------|--------|------|
| id | BIGINT | 是 | 自增 | 图片唯一标识 |
| product_id | BIGINT | 是 | - | 商品ID（外键） |
| image_url | VARCHAR(255) | 是 | - | 图片URL |
| image_type | TINYINT | 否 | 1 | 图片类型：1-主图，2-详情图，3-规格图 |
| sort | INT | 否 | 0 | 排序（数字越小越靠前） |
| create_time | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 否 | CURRENT_TIMESTAMP | 更新时间 |
| is_deleted | TINYINT | 否 | 0 | 逻辑删除：0-未删除，1-已删除 |

**索引设计：**
- 主键索引：`id`
- 普通索引：`product_id`、`image_type`

---

### 11. 订单详情表 (order_item)

**表说明：** 记录订单中的商品明细信息

| 字段名 | 类型 | 是否必填 | 默认值 | 说明 |
|--------|------|----------|--------|------|
| id | BIGINT | 是 | 自增 | 订单详情唯一标识 |
| order_id | BIGINT | 是 | - | 订单ID（外键） |
| order_no | VARCHAR(64) | 是 | - | 订单号 |
| product_id | BIGINT | 是 | - | 商品ID（外键） |
| product_name | VARCHAR(200) | 是 | - | 商品名称（冗余字段） |
| product_image | VARCHAR(255) | 否 | NULL | 商品主图（冗余字段） |
| product_price | DECIMAL(10,2) | 是 | - | 商品单价（下单时的价格） |
| quantity | INT | 是 | - | 购买数量 |
| total_price | DECIMAL(10,2) | 是 | - | 小计金额 |
| create_time | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 否 | CURRENT_TIMESTAMP | 更新时间 |

**设计说明：**
- 冗余商品名称、图片、价格等信息，防止商品修改影响历史订单
- `product_price` 记录下单时的价格，不受后续商品价格变动影响

**索引设计：**
- 主键索引：`id`
- 普通索引：`order_id`、`order_no`、`product_id`