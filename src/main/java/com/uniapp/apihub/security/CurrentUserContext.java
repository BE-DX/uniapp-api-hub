package com.uniapp.apihub.security;

import cn.dev33.satoken.stp.StpUtil;
import com.uniapp.apihub.common.BusinessException;
import com.uniapp.apihub.module.auth.entity.Company;
import com.uniapp.apihub.module.auth.entity.User;
import com.uniapp.apihub.module.auth.mapper.CompanyMapper;
import com.uniapp.apihub.module.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 当前请求上下文。
 *
 * 统一封装当前登录用户、角色判断和中台能力权限判断，避免业务代码到处直接读取 Sa-Token。
 */
@Service
@RequiredArgsConstructor
public class CurrentUserContext {

    private final UserMapper userMapper;
    private final CompanyMapper companyMapper;

    public CurrentUser currentUser() {
        User user = currentUserEntity();
        return CurrentUser.from(user, StpUtil.getPermissionList());
    }

    public User currentUserEntity() {
        StpUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if ("B".equals(user.getForbidStatus())) {
            StpUtil.logout();
            throw new BusinessException(401, "账号已被禁用，请重新登录");
        }
        enrichCompanyName(user);
        return user;
    }

    public Long currentUserId() {
        return currentUserEntity().getId();
    }

    public boolean isAdmin() {
        return UserRoles.isAdminRole(currentUserEntity().getRole());
    }

    public void requireAdmin() {
        if (!isAdmin()) {
            throw new BusinessException(403, "需要管理员权限");
        }
    }

    public void requireSelfOrAdmin(Long userId) {
        CurrentUser current = currentUser();
        if (!current.isAdmin() && !current.getUserId().equals(userId)) {
            throw new BusinessException(403, "只能访问自己的用户信息");
        }
    }

    public boolean hasHubPermission(String sysCode, String operation) {
        String exact = sysCode + ":" + operation;
        List<String> permissions = StpUtil.getPermissionList();
        return permissions.contains("*:*")
                || permissions.contains(exact)
                || permissions.contains(sysCode + ":*")
                || permissions.contains("*:" + operation);
    }

    public void requireHubPermission(String sysCode, String operation) {
        if (!hasHubPermission(sysCode, operation)) {
            throw new BusinessException(403, "无权访问中台能力: " + sysCode + ":" + operation);
        }
    }

    public Map<String, Object> toUserInfo(User user) {
        enrichCompanyName(user);
        return toUserInfo(CurrentUser.from(user, StpUtil.isLogin() ? StpUtil.getPermissionList() : null));
    }

    public Map<String, Object> toUserInfo(CurrentUser user) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("userId", user.getUserId());
        info.put("username", user.getUsername());
        info.put("realName", user.getRealName());
        info.put("companyId", user.getCompanyId());
        info.put("companyName", user.getCompanyName());
        info.put("role", user.getRole());
        info.put("phone", user.getPhone());
        info.put("email", user.getEmail());
        info.put("isAdmin", user.isAdmin());
        info.put("isSuperAdmin", user.isSuperAdmin());
        info.put("permissions", user.getPermissions());
        return info;
    }

    private void enrichCompanyName(User user) {
        if (user == null || user.getCompanyId() == null || user.getCompanyName() != null) {
            return;
        }
        Company company = companyMapper.selectById(user.getCompanyId());
        if (company != null) {
            user.setCompanyName(company.getCompanyName());
        }
    }
}
