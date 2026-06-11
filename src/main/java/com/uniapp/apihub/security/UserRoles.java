package com.uniapp.apihub.security;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 系统内置角色。
 *
 * 当前阶段不引入角色表，先用少量固定角色承载权限边界；
 * 后续如果升级为 RBAC，这里可以作为兼容旧数据的角色编码入口。
 */
public final class UserRoles {

    public static final String SUPER_ADMIN = "superAdmin";
    public static final String ADMIN = "admin";
    public static final String USER = "user";

    private static final Set<String> VALID_ROLES = new HashSet<>(Arrays.asList(
            SUPER_ADMIN, ADMIN, USER
    ));

    private UserRoles() {
    }

    public static boolean isValid(String role) {
        return role != null && VALID_ROLES.contains(role);
    }

    public static boolean isSuperAdmin(String role) {
        return SUPER_ADMIN.equals(role);
    }

    public static boolean isAdminRole(String role) {
        return SUPER_ADMIN.equals(role) || ADMIN.equals(role);
    }
}
