package com.uniapp.apihub.module.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表 — 序列化使用 K3Cloud 字段名以兼容现有前端页面
 */
@Data
@TableName("sys_user")
public class User {

    @TableId(type = IdType.AUTO)
    @JsonProperty("FUserID")
    @com.fasterxml.jackson.annotation.JsonAlias({"id", "userId"})
    private Long id;

    /** 用户名 */
    @JsonProperty("FUserName")
    @com.fasterxml.jackson.annotation.JsonAlias({"username"})
    private String username;

    /** 密码(加密后) — 不返回给前端 */
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String password;

    /** 密码盐 — 不返回给前端 */
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String salt;

    /** 真实姓名 */
    @JsonProperty("FRealName")
    @com.fasterxml.jackson.annotation.JsonAlias({"realName", "FRealName"})
    private String realName;

    /** 手机号 */
    @JsonProperty("FPhone")
    @com.fasterxml.jackson.annotation.JsonAlias({"phone"})
    private String phone;

    /** 邮箱 */
    @JsonProperty("FEmail")
    @com.fasterxml.jackson.annotation.JsonAlias({"email"})
    private String email;

    /** 所属公司ID */
    @JsonProperty("FCompanyID")
    @com.fasterxml.jackson.annotation.JsonAlias({"companyId"})
    private Long companyId;

    /** 所属公司名称，仅返回前端展示 */
    @TableField(exist = false)
    @JsonProperty("FCompanyName")
    @com.fasterxml.jackson.annotation.JsonAlias({"companyName"})
    private String companyName;

    /** 角色: superAdmin / admin / user */
    @JsonProperty("FRole")
    @com.fasterxml.jackson.annotation.JsonAlias({"role"})
    private String role;

    /** 禁用状态: A-正常 / B-禁用 */
    @JsonProperty("FForbidStatus")
    @com.fasterxml.jackson.annotation.JsonAlias({"forbidStatus"})
    private String forbidStatus;

    /** 最后登录时间 */
    @JsonProperty("FLastLoginTime")
    @com.fasterxml.jackson.annotation.JsonAlias({"lastLoginTime"})
    private LocalDateTime lastLoginTime;

    @TableField(fill = FieldFill.INSERT)
    @JsonProperty("FCreateTime")
    @com.fasterxml.jackson.annotation.JsonAlias({"createTime"})
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonIgnore
    private LocalDateTime updateTime;

    @TableLogic
    @JsonIgnore
    private Integer deleted;
}
