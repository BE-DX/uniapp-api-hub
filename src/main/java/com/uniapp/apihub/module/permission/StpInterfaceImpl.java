package com.uniapp.apihub.module.permission;

import cn.dev33.satoken.stp.StpInterface;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uniapp.apihub.module.auth.entity.User;
import com.uniapp.apihub.module.auth.mapper.UserMapper;
import com.uniapp.apihub.module.permission.entity.RolePermission;
import com.uniapp.apihub.module.permission.mapper.RolePermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
        if ("superAdmin".equals(user.getRole())) {
            return rolePermissionMapper.selectList(null).stream()
                    .map(p -> buildPermissionKey(p.getSysCode(), p.getRouteKey()))
                    .collect(Collectors.toList());
        }

        // 查该角色对应的权限
        List<RolePermission> permissions = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>()
                        .eq(RolePermission::getRoleCode, user.getRole()));
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
