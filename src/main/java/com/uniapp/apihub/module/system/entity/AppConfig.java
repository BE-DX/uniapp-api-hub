package com.uniapp.apihub.module.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 应用配置表 — 键值对存储
 */
@Data
@TableName("sys_app_config")
public class AppConfig {

    @TableId
    private String configKey;

    private String configValue;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
