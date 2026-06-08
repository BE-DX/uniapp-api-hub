-- =========================================
-- Uniapp API Hub — 数据库初始化脚本
-- =========================================

CREATE DATABASE IF NOT EXISTS uniapp_api_hub
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE uniapp_api_hub;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(128) NOT NULL COMMENT '密码(加密后)',
    salt VARCHAR(64) NOT NULL COMMENT '密码盐',
    real_name VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    role VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色: superAdmin/admin/user',
    forbid_status CHAR(1) NOT NULL DEFAULT 'A' COMMENT '禁用状态: A-正常/B-禁用',
    last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS sys_system_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sys_code VARCHAR(50) NOT NULL COMMENT '系统编码，如 k3cloud/cosmic',
    sys_name VARCHAR(100) NOT NULL COMMENT '系统名称',
    base_url VARCHAR(500) NOT NULL COMMENT '下游系统基础URL',
    auth_type VARCHAR(20) NOT NULL DEFAULT 'NONE' COMMENT '认证方式: TOKEN/COOKIE/BASIC/NONE',
    auth_config JSON DEFAULT NULL COMMENT '认证配置JSON',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_sys_code (sys_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- API路由表
CREATE TABLE IF NOT EXISTS sys_api_route (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    system_id BIGINT NOT NULL COMMENT '关联系统ID',
    route_key VARCHAR(50) NOT NULL COMMENT '路由标识，如 save/query/view',
    target_path VARCHAR(500) NOT NULL COMMENT '下游实际请求路径',
    http_method VARCHAR(10) NOT NULL DEFAULT 'POST' COMMENT 'HTTP方法',
    req_transform JSON DEFAULT NULL COMMENT '请求转换模板JSON',
    resp_transform JSON DEFAULT NULL COMMENT '响应转换模板JSON',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_sys_route (system_id, route_key),
    INDEX idx_system_id (system_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API路由表';

-- 角色权限表
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_code VARCHAR(20) NOT NULL COMMENT '角色: superAdmin/admin/user',
    sys_code VARCHAR(50) DEFAULT NULL COMMENT '系统编码(null=所有)',
    route_key VARCHAR(50) DEFAULT NULL COMMENT '路由标识(null=该系统下所有)',
    allowed TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_role (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限表';

-- =========================================
-- 初始化数据
-- =========================================

-- 默认角色权限: user角色默认不允许任何操作（管理员手工分配）
-- admin角色示例: 允许访问 k3cloud 系统
INSERT INTO sys_role_permission (role_code, sys_code, route_key, allowed) VALUES
('superAdmin', NULL, NULL, 1);
