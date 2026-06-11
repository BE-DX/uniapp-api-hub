package com.uniapp.apihub.module.permission.entity;

import lombok.Data;

import java.util.List;

/**
 * 保存用户授权请求。
 */
@Data
public class SaveUserPermissionsDTO {

    private Long userId;

    private List<String> permissions;
}
