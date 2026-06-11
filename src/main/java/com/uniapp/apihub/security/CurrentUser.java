package com.uniapp.apihub.security;

import com.uniapp.apihub.module.auth.entity.User;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 当前请求用户上下文快照。
 */
@Getter
public class CurrentUser {

    private final Long userId;
    private final String username;
    private final String realName;
    private final Long companyId;
    private final String companyName;
    private final String role;
    private final String phone;
    private final String email;
    private final boolean superAdmin;
    private final boolean admin;
    private final List<String> permissions;

    private CurrentUser(User user, List<String> permissions) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.realName = user.getRealName();
        this.companyId = user.getCompanyId();
        this.companyName = user.getCompanyName();
        this.role = user.getRole();
        this.phone = user.getPhone();
        this.email = user.getEmail();
        this.superAdmin = UserRoles.isSuperAdmin(user.getRole());
        this.admin = UserRoles.isAdminRole(user.getRole());
        this.permissions = Collections.unmodifiableList(new ArrayList<>(permissions));
    }

    public static CurrentUser from(User user, List<String> permissions) {
        return new CurrentUser(user, permissions == null ? Collections.emptyList() : permissions);
    }
}
