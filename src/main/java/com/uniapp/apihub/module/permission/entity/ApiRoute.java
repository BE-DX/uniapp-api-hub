package com.uniapp.apihub.module.permission.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API 路由能力。
 */
@Data
@TableName("sys_api_route")
public class ApiRoute {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long systemId;

    private String routeKey;

    private String targetPath;

    private String httpMethod;

    private Boolean enabled;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
