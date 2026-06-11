package com.uniapp.apihub.module.permission;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.uniapp.apihub.common.ApiResponse;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uniapp.apihub.module.auth.entity.User;
import com.uniapp.apihub.module.permission.entity.SaveRolePermissionsDTO;
import com.uniapp.apihub.module.permission.entity.SaveUserPermissionsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 授权管理接口。
 */
@RestController
@RequestMapping("/api/permission")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @SaCheckRole(value = {"superAdmin", "admin"}, mode = SaMode.OR)
    @GetMapping("/options")
    public ApiResponse<List<Map<String, Object>>> options() {
        return ApiResponse.ok(permissionService.listPermissionOptions());
    }

    @SaCheckRole(value = {"superAdmin", "admin"}, mode = SaMode.OR)
    @GetMapping("/role/{roleCode}")
    public ApiResponse<List<String>> rolePermissions(@PathVariable String roleCode) {
        return ApiResponse.ok(permissionService.listRolePermissions(roleCode));
    }

    @SaCheckRole(value = {"superAdmin", "admin"}, mode = SaMode.OR)
    @GetMapping("/users")
    public ApiResponse<Page<User>> users(@RequestParam(defaultValue = "") String keyword,
                                         @RequestParam(defaultValue = "") String role,
                                         @RequestParam(defaultValue = "") String status,
                                         @RequestParam(required = false) Long companyId,
                                         @RequestParam(defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(permissionService.listGrantUsers(keyword, role, status, companyId, pageNum, pageSize));
    }

    @SaCheckRole(value = {"superAdmin", "admin"}, mode = SaMode.OR)
    @GetMapping("/user/{userId}")
    public ApiResponse<List<String>> userPermissions(@PathVariable Long userId) {
        return ApiResponse.ok(permissionService.listUserPermissions(userId));
    }

    @SaCheckRole("superAdmin")
    @PutMapping("/role")
    public ApiResponse<List<String>> saveRolePermissions(@RequestBody SaveRolePermissionsDTO dto) {
        return ApiResponse.ok(permissionService.saveRolePermissions(dto.getRoleCode(), dto.getPermissions()));
    }

    @SaCheckRole("superAdmin")
    @PutMapping("/user")
    public ApiResponse<List<String>> saveUserPermissions(@RequestBody SaveUserPermissionsDTO dto) {
        return ApiResponse.ok(permissionService.saveUserPermissions(dto.getUserId(), dto.getPermissions()));
    }
}
