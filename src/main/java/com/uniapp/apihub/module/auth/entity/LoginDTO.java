package com.uniapp.apihub.module.auth.entity;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 登录请求DTO
 */
@Data
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 客户端类型：pc / mobile / miniapp。
     */
    private String clientType;

    /**
     * 是否确认顶掉同客户端旧会话。
     */
    private Boolean forceLogin;
}
