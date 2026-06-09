-- 应用配置表（键值对）
CREATE TABLE IF NOT EXISTS sys_app_config (
    config_key VARCHAR(100) NOT NULL COMMENT '配置键',
    config_value TEXT NOT NULL COMMENT '配置值',
    remark VARCHAR(200) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用配置表';

-- 插入默认配置
INSERT IGNORE INTO sys_app_config (config_key, config_value, remark) VALUES
('defaultPassword', 'password123', '新用户和重置密码时的默认密码');
