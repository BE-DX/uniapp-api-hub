package com.uniapp.apihub.module.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 下游系统配置表 — 每种类型仅一条记录，物理删除
 */
@Data
@TableName("sys_system_config")
public class SystemConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 系统编码，如 k3cloud / jdy */
    private String sysCode;

    /** 系统名称 */
    private String sysName;

    /** 下游系统基础URL */
    private String baseUrl;

    /** 认证方式: TOKEN / COOKIE / BASIC / NONE */
    private String authType;

    /** 认证配置JSON: {appId, appSec, cookieName, username, password, ...} */
    private String authConfig;

    /** 是否启用 */
    private Boolean enabled;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
