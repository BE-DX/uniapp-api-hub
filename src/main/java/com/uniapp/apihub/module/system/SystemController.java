package com.uniapp.apihub.module.system;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import com.uniapp.apihub.common.ApiResponse;
import com.uniapp.apihub.common.BusinessException;
import com.uniapp.apihub.module.auth.entity.User;
import com.uniapp.apihub.module.auth.mapper.UserMapper;
import com.uniapp.apihub.module.system.entity.SystemConfig;
import com.uniapp.apihub.module.system.enums.SystemTypeEnum;
import com.uniapp.apihub.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统管理控制器
 *
 * 权限控制：所有管理端点需 admin 或 superAdmin 角色
 * 敏感操作：查看/编辑 authConfig 需二次密码验证
 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final SystemService systemService;
    private final AppConfigService appConfigService;
    private final UserMapper userMapper;

    /* ==================== 应用配置 ==================== */

    @SaCheckLogin
    @GetMapping("/config")
    public ApiResponse<Map<String, String>> getAppConfig() {
        return ApiResponse.ok(appConfigService.getAllConfig());
    }

    @SaCheckRole(value = {"admin", "superAdmin"}, mode = SaMode.OR)
    @PutMapping("/config")
    public ApiResponse<Map<String, String>> saveAppConfig(@RequestBody Map<String, String> config) {
        return ApiResponse.ok(appConfigService.saveConfig(config));
    }

    /* ==================== 系统配置 ==================== */

    @SaCheckRole(value = {"admin", "superAdmin"}, mode = SaMode.OR)
    @GetMapping("/list")
    public ApiResponse<List<SystemConfig>> listSystems() {
        return ApiResponse.ok(systemService.listSystems());
    }

    /** 获取可用的系统类型枚举（前端下拉用） */
    @SaCheckLogin
    @GetMapping("/types")
    public ApiResponse<List<Map<String, String>>> getSystemTypes() {
        return ApiResponse.ok(SystemTypeEnum.toVoList());
    }

    @SaCheckRole(value = {"admin", "superAdmin"}, mode = SaMode.OR)
    @GetMapping("/{sysCode}")
    public ApiResponse<SystemConfig> getSystem(@PathVariable String sysCode) {
        return ApiResponse.ok(systemService.getSystem(sysCode));
    }

    @SaCheckRole(value = {"admin", "superAdmin"}, mode = SaMode.OR)
    @PostMapping("/add")
    public ApiResponse<SystemConfig> addSystem(@RequestBody SystemConfig config) {
        return ApiResponse.ok(systemService.addSystem(config));
    }

    @SaCheckRole(value = {"admin", "superAdmin"}, mode = SaMode.OR)
    @PutMapping("/update")
    public ApiResponse<SystemConfig> updateSystem(@RequestBody SystemConfig config) {
        return ApiResponse.ok(systemService.updateSystem(config));
    }

    @SaCheckRole(value = {"admin", "superAdmin"}, mode = SaMode.OR)
    @DeleteMapping("/delete/{id}")
    public ApiResponse<?> deleteSystem(@PathVariable Long id) {
        systemService.deleteSystem(id);
        return ApiResponse.ok();
    }

    /* ==================== 二次验证 ==================== */

    /**
     * 二次密码验证后查看解密后的 authConfig
     *
     * 请求体：{"password": "当前管理员密码"}
     * 响应：解密后的 authConfig JSON 字符串
     */
    @SaCheckRole(value = {"admin", "superAdmin"}, mode = SaMode.OR)
    @PostMapping("/reveal-auth/{sysCode}")
    public ApiResponse<String> revealAuthConfig(@PathVariable String sysCode,
                                                 @RequestBody Map<String, String> body) {
        String password = body.get("password");
        if (password == null || password.isEmpty()) {
            throw new BusinessException("请输入当前密码进行身份验证");
        }

        // 验密
        long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!PasswordUtil.verify(password, user.getSalt(), user.getPassword())) {
            throw new BusinessException("密码验证失败，无法查看敏感信息");
        }

        // 返回解密后的 authConfig
        String plainAuth = systemService.getAuthConfigPlain(sysCode);
        return ApiResponse.ok(plainAuth);
    }
}
