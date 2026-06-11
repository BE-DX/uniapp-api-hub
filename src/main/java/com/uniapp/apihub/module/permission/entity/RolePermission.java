package com.uniapp.apihub.module.permission.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色权限表
 */
@Data
@TableName("sys_role_permission")
public class RolePermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色: superAdmin / admin / user */
    private String roleCode;

    /** 授权主体类型: role / user，当前管理端先使用 role */
    private String subjectType;

    /** 授权主体编码: roleCode 或用户ID，当前管理端先使用 roleCode */
    private String subjectCode;

    /** 系统编码（null表示所有系统） */
    private String sysCode;

    /** 业务模块编码（预留，null表示不按模块细分） */
    private String moduleCode;

    /** 路由标识（null表示该系统下所有路由） */
    private String routeKey;

    /** 是否允许 */
    private Boolean allowed;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
