package com.uniapp.apihub.module.auth;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uniapp.apihub.common.ApiResponse;
import com.uniapp.apihub.module.auth.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器 — 操作中台自有数据库，不依赖下游系统
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 用户列表 */
    @SaCheckLogin
    @GetMapping("/list")
    public ApiResponse<Page<User>> list(@RequestParam(defaultValue = "") String keyword,
                                         @RequestParam(defaultValue = "") String role,
                                         @RequestParam(defaultValue = "") String status,
                                         @RequestParam(required = false) Long companyId,
                                         @RequestParam(defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(userService.listUsers(keyword, role, status, companyId, pageNum, pageSize));
    }

    /** 用户详情 */
    @SaCheckLogin
    @GetMapping("/{id}")
    public ApiResponse<User> getById(@PathVariable Long id) {
        return ApiResponse.ok(userService.getUserById(id));
    }

    /** 创建用户 — 管理员权限 */
    @SaCheckLogin
    @SaCheckRole(value = {"superAdmin", "admin"}, mode = SaMode.OR)
    @PostMapping("/create")
    public ApiResponse<User> create(@RequestBody User user) {
        return ApiResponse.ok(userService.createUser(user));
    }

    /** 更新用户 */
    @SaCheckLogin
    @PutMapping("/update")
    public ApiResponse<User> update(@RequestBody User user) {
        return ApiResponse.ok(userService.updateUser(user));
    }

    /** 删除用户 */
    @SaCheckLogin
    @SaCheckRole(value = {"superAdmin", "admin"}, mode = SaMode.OR)
    @DeleteMapping("/delete/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.ok();
    }

    /** 设置启用/禁用 */
    @SaCheckLogin
    @SaCheckRole(value = {"superAdmin", "admin"}, mode = SaMode.OR)
    @PostMapping("/setStatus")
    public ApiResponse<?> setStatus(@RequestParam Long id, @RequestParam String status) {
        userService.setUserStatus(id, status);
        return ApiResponse.ok();
    }

    /** 重置密码（管理员操作） */
    @SaCheckLogin
    @SaCheckRole(value = {"superAdmin", "admin"}, mode = SaMode.OR)
    @PostMapping("/resetPassword")
    public ApiResponse<?> resetPassword(@RequestParam Long id) {
        userService.resetPassword(id);
        return ApiResponse.ok();
    }
}
