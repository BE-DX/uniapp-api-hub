package com.uniapp.apihub.module.permission;

import cn.dev33.satoken.stp.StpInterface;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uniapp.apihub.module.auth.entity.User;
import com.uniapp.apihub.module.auth.mapper.UserMapper;
import com.uniapp.apihub.module.permission.entity.RolePermission;
import com.uniapp.apihub.module.permission.mapper.RolePermissionMapper;
import com.uniapp.apihub.security.UserRoles;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sa-Token 权限接口实现 — 加载用户角色和权限
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final UserMapper userMapper;
    private final RolePermissionMapper rolePermissionMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        long userId = Long.parseLong(loginId.toString());
        User user = userMapper.selectById(userId);
        if (user == null) {
            return new ArrayList<>();
        }

        // 超级管理员拥有所有权限
        if (UserRoles.isSuperAdmin(user.getRole())) {
            return Collections.singletonList("*:*");
        }

        // 查角色基线权限 + 用户个人追加权限
        List<RolePermission> permissions = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>()
                        .and(q -> q.and(role -> role
                                        .eq(RolePermission::getSubjectType, "role")
                                        .eq(RolePermission::getSubjectCode, user.getRole()))
                                .or(userScope -> userScope
                                        .eq(RolePermission::getSubjectType, "user")
                                        .eq(RolePermission::getSubjectCode, String.valueOf(userId)))
                                .or(legacy -> legacy
                                        .isNull(RolePermission::getSubjectType)
                                        .eq(RolePermission::getRoleCode, user.getRole())))
                        .eq(RolePermission::getAllowed, true));
        return permissions.stream()
                .map(p -> buildPermissionKey(p.getSysCode(), p.getRouteKey()))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        long userId = Long.parseLong(loginId.toString());
        User user = userMapper.selectById(userId);
        List<String> roles = new ArrayList<>();
        if (user != null) {
            roles.add(user.getRole());
        }
        return roles;
    }

    /**
     * 权限标识: sysCode:routeKey 格式，如 k3cloud:save
     */
    private String buildPermissionKey(String sysCode, String routeKey) {
        if (sysCode == null && routeKey == null) {
            return "*:*";
        }
        if (routeKey == null) {
            return sysCode + ":*";
        }
        return sysCode + ":" + routeKey;
    }
}
