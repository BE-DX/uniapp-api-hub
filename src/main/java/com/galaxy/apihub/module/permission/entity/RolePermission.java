package com.galaxy.apihub.module.permission.entity;

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

    /** 系统编码（null表示所有系统） */
    private String sysCode;

    /** 路由标识（null表示该系统下所有路由） */
    private String routeKey;

    /** 是否允许 */
    private Boolean allowed;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
