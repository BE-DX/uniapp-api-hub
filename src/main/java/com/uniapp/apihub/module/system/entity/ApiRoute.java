package com.uniapp.apihub.module.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API路由表 — 中台路径 → 下游实际路径的映射
 */
@Data
@TableName("sys_api_route")
public class ApiRoute {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联系统ID */
    private Long systemId;

    /** 中台暴露的路径标识，如 save / query */
    private String routeKey;

    /** 下游实际请求路径 */
    private String targetPath;

    /** HTTP方法: GET / POST / PUT / DELETE */
    private String httpMethod;

    /** 请求转换模板JSON（可选） */
    private String reqTransform;

    /** 响应转换模板JSON（可选） */
    private String respTransform;

    /** 是否启用 */
    private Boolean enabled;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
