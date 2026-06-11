-- 公司信息与用户归属。
CREATE TABLE IF NOT EXISTS sys_company (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_code VARCHAR(50) NOT NULL COMMENT '公司编码',
    company_name VARCHAR(100) NOT NULL COMMENT '公司名称',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_company_code (company_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司表';

INSERT INTO sys_company (company_code, company_name, enabled, remark)
SELECT 'default', '默认公司', 1, '系统默认公司'
WHERE NOT EXISTS (SELECT 1 FROM sys_company WHERE company_code = 'default');

ALTER TABLE sys_user
    ADD COLUMN company_id BIGINT DEFAULT NULL COMMENT '所属公司ID' AFTER email,
    ADD INDEX idx_company_id (company_id);

UPDATE sys_user
SET company_id = (SELECT id FROM sys_company WHERE company_code = 'default' LIMIT 1)
WHERE company_id IS NULL;
