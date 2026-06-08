package com.galaxy.apihub.module.auth;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.galaxy.apihub.common.ApiResponse;
import com.galaxy.apihub.module.auth.entity.ChangePwdDTO;
import com.galaxy.apihub.module.auth.entity.LoginDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 登录 — 公开接口 */
    @PostMapping("/login")
    public ApiResponse<?> login(@Valid @RequestBody LoginDTO dto) {
        return ApiResponse.ok(authService.login(dto));
    }

    /** 登出 — 需登录 */
    @SaCheckLogin
    @PostMapping("/logout")
    public ApiResponse<?> logout() {
        authService.logout();
        return ApiResponse.ok();
    }

    /** 初始化超级管理员 — 公开接口（仅首次使用） */
    @PostMapping("/initAdmin")
    public ApiResponse<?> initAdmin() {
        return ApiResponse.ok(authService.initAdmin());
    }

    /** 获取当前用户 — 需登录 */
    @SaCheckLogin
    @GetMapping("/currentUser")
    public ApiResponse<?> currentUser() {
        return ApiResponse.ok(authService.currentUser());
    }

    /** 修改密码 — 需登录 */
    @SaCheckLogin
    @PostMapping("/changePassword")
    public ApiResponse<?> changePassword(@Valid @RequestBody ChangePwdDTO dto) {
        authService.changePassword(dto.getOldPwd(), dto.getNewPwd());
        return ApiResponse.ok();
    }
}
