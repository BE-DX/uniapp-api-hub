package com.uniapp.apihub.module.auth.entity;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 修改密码请求DTO
 */
@Data
public class ChangePwdDTO {

    @NotBlank(message = "旧密码不能为空")
    private String oldPwd;

    @NotBlank(message = "新密码不能为空")
    private String newPwd;
}
