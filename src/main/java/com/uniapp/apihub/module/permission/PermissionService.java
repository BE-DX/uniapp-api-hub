package com.uniapp.apihub.module.permission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uniapp.apihub.common.BusinessException;
import com.uniapp.apihub.module.auth.entity.Company;
import com.uniapp.apihub.module.auth.entity.User;
import com.uniapp.apihub.module.auth.mapper.CompanyMapper;
import com.uniapp.apihub.module.auth.mapper.UserMapper;
import com.uniapp.apihub.module.permission.entity.ApiRoute;
import com.uniapp.apihub.module.permission.entity.RolePermission;
import com.uniapp.apihub.module.permission.mapper.ApiRouteMapper;
import com.uniapp.apihub.module.permission.mapper.RolePermissionMapper;
import com.uniapp.apihub.module.system.entity.SystemConfig;
import com.uniapp.apihub.module.system.mapper.SystemConfigMapper;
import com.uniapp.apihub.security.UserRoles;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 授权管理服务。
 *
 * 当前先管理角色到系统/API能力的授权；moduleCode 字段预留给后续业务模块分组。
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private static final String SUBJECT_ROLE = "role";
    private static final String SUBJECT_USER = "user";

    private final UserMapper userMapper;
    private final CompanyMapper companyMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final ApiRouteMapper apiRouteMapper;
    private final RolePermissionMapper rolePermissionMapper;

    public List<Map<String, Object>> listPermissionOptions() {
        List<SystemConfig> systems = systemConfigMapper.selectList(
                new LambdaQueryWrapper<SystemConfig>()
                        .eq(SystemConfig::getEnabled, true)
                        .orderByAsc(SystemConfig::getId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (SystemConfig system : systems) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sysCode", system.getSysCode());
            item.put("sysName", system.getSysName());
            item.put("permission", system.getSysCode() + ":*");

            List<ApiRoute> routes = apiRouteMapper.selectList(
                    new LambdaQueryWrapper<ApiRoute>()
                            .eq(ApiRoute::getSystemId, system.getId())
                            .eq(ApiRoute::getEnabled, true)
                            .orderByAsc(ApiRoute::getId));
            List<Map<String, Object>> routeItems = routes.stream().map(route -> {
                Map<String, Object> routeItem = new LinkedHashMap<>();
                routeItem.put("routeKey", route.getRouteKey());
                routeItem.put("name", route.getRemark() == null || route.getRemark().isEmpty()
                        ? route.getRouteKey()
                        : route.getRemark());
                routeItem.put("permission", system.getSysCode() + ":" + route.getRouteKey());
                return routeItem;
            }).collect(Collectors.toList());
            item.put("routes", routeItems);
            result.add(item);
        }
        return result;
    }

    public List<String> listRolePermissions(String roleCode) {
        validateManagedRole(roleCode);
        return rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<RolePermission>()
                                .eq(RolePermission::getSubjectType, SUBJECT_ROLE)
                                .eq(RolePermission::getSubjectCode, roleCode)
                                .eq(RolePermission::getAllowed, true))
                .stream()
                .map(this::buildPermissionKey)
                .collect(Collectors.toList());
    }

    public List<String> listUserPermissions(Long userId) {
        validateUser(userId);
        return rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<RolePermission>()
                                .eq(RolePermission::getSubjectType, SUBJECT_USER)
                                .eq(RolePermission::getSubjectCode, String.valueOf(userId))
                                .eq(RolePermission::getAllowed, true))
                .stream()
                .map(this::buildPermissionKey)
                .collect(Collectors.toList());
    }

    public Page<User> listGrantUsers(String keyword, String role, String status, Long companyId, int pageNum, int pageSize) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.ne(User::getRole, UserRoles.SUPER_ADMIN);
        if (keyword != null && !keyword.trim().isEmpty()) {
            qw.and(q -> q.like(User::getUsername, keyword)
                    .or()
                    .like(User::getRealName, keyword)
                    .or()
                    .like(User::getPhone, keyword)
                    .or()
                    .like(User::getEmail, keyword));
        }
        if (role != null && !role.isEmpty()) {
            qw.eq(User::getRole, role);
        }
        if (status != null && !status.isEmpty()) {
            qw.eq(User::getForbidStatus, status);
        }
        if (companyId != null) {
            qw.eq(User::getCompanyId, companyId);
        }
        qw.orderByDesc(User::getId);
        Page<User> page = userMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        for (User user : page.getRecords()) {
            enrichCompanyName(user);
            user.setPassword(null);
            user.setSalt(null);
        }
        return page;
    }

    @Transactional
    public List<String> saveRolePermissions(String roleCode, List<String> permissions) {
        validateManagedRole(roleCode);

        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getSubjectType, SUBJECT_ROLE)
                .eq(RolePermission::getSubjectCode, roleCode));

        if (permissions != null) {
            for (String permission : permissions) {
                RolePermission entity = parsePermission(roleCode, permission);
                rolePermissionMapper.insert(entity);
            }
        }

        return listRolePermissions(roleCode);
    }

    @Transactional
    public List<String> saveUserPermissions(Long userId, List<String> permissions) {
        User user = validateUser(userId);

        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getSubjectType, SUBJECT_USER)
                .eq(RolePermission::getSubjectCode, String.valueOf(userId)));

        if (permissions != null) {
            for (String permission : permissions) {
                RolePermission entity = parsePermission(user.getRole(), permission);
                entity.setSubjectType(SUBJECT_USER);
                entity.setSubjectCode(String.valueOf(userId));
                rolePermissionMapper.insert(entity);
            }
        }

        return listUserPermissions(userId);
    }

    private void validateManagedRole(String roleCode) {
        if (!UserRoles.ADMIN.equals(roleCode) && !UserRoles.USER.equals(roleCode)) {
            throw new BusinessException("仅支持配置 admin/user 角色授权，superAdmin 默认拥有全部权限");
        }
    }

    private User validateUser(Long userId) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (UserRoles.SUPER_ADMIN.equals(user.getRole())) {
            throw new BusinessException("superAdmin 默认拥有全部权限，无需单独授权");
        }
        return user;
    }

    private RolePermission parsePermission(String roleCode, String permission) {
        if (permission == null || permission.trim().isEmpty()) {
            throw new BusinessException("权限标识不能为空");
        }

        String[] parts = permission.trim().split(":");
        if (parts.length != 2) {
            throw new BusinessException("权限标识格式错误: " + permission);
        }

        RolePermission entity = new RolePermission();
        entity.setRoleCode(roleCode);
        entity.setSubjectType(SUBJECT_ROLE);
        entity.setSubjectCode(roleCode);
        entity.setAllowed(true);

        if ("*".equals(parts[0]) && "*".equals(parts[1])) {
            entity.setSysCode(null);
            entity.setRouteKey(null);
            return entity;
        }

        entity.setSysCode(parts[0]);
        entity.setRouteKey("*".equals(parts[1]) ? null : parts[1]);
        return entity;
    }

    private String buildPermissionKey(RolePermission permission) {
        if (permission.getSysCode() == null && permission.getRouteKey() == null) {
            return "*:*";
        }
        if (permission.getRouteKey() == null) {
            return permission.getSysCode() + ":*";
        }
        return permission.getSysCode() + ":" + permission.getRouteKey();
    }

    private void enrichCompanyName(User user) {
        if (user == null || user.getCompanyId() == null) {
            return;
        }
        Company company = companyMapper.selectById(user.getCompanyId());
        if (company != null) {
            user.setCompanyName(company.getCompanyName());
        }
    }
}
