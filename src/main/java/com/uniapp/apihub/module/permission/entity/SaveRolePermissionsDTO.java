package com.uniapp.apihub.module.permission.entity;

import lombok.Data;

import java.util.List;

/**
 * 保存角色授权请求。
 */
@Data
public class SaveRolePermissionsDTO {

    private String roleCode;

    private List<String> permissions;
}
