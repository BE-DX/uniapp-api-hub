-- 授权表升级：支持角色/用户授权主体，并预留业务模块字段。
ALTER TABLE sys_role_permission
    ADD COLUMN subject_type VARCHAR(20) NOT NULL DEFAULT 'role' COMMENT '授权主体类型: role/user' AFTER role_code,
    ADD COLUMN subject_code VARCHAR(50) DEFAULT NULL COMMENT '授权主体编码: 角色编码或用户ID' AFTER subject_type,
    ADD COLUMN module_code VARCHAR(50) DEFAULT NULL COMMENT '业务模块编码(预留)' AFTER sys_code;

UPDATE sys_role_permission
SET subject_type = 'role',
    subject_code = role_code
WHERE subject_code IS NULL;

CREATE INDEX idx_permission_subject ON sys_role_permission (subject_type, subject_code);
