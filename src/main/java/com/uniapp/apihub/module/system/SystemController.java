package com.uniapp.apihub.module.system;

import com.uniapp.apihub.common.ApiResponse;
import com.uniapp.apihub.module.system.entity.ApiRoute;
import com.uniapp.apihub.module.system.entity.SystemConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统管理控制器
 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final SystemService systemService;

    /* ==================== 系统配置 ==================== */

    @GetMapping("/list")
    public ApiResponse<List<SystemConfig>> listSystems() {
        return ApiResponse.ok(systemService.listSystems());
    }

    @GetMapping("/{sysCode}")
    public ApiResponse<SystemConfig> getSystem(@PathVariable String sysCode) {
        return ApiResponse.ok(systemService.getSystem(sysCode));
    }

    @PostMapping("/add")
    public ApiResponse<SystemConfig> addSystem(@RequestBody SystemConfig config) {
        return ApiResponse.ok(systemService.addSystem(config));
    }

    @PutMapping("/update")
    public ApiResponse<SystemConfig> updateSystem(@RequestBody SystemConfig config) {
        return ApiResponse.ok(systemService.updateSystem(config));
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<?> deleteSystem(@PathVariable Long id) {
        systemService.deleteSystem(id);
        return ApiResponse.ok();
    }

    /* ==================== API路由 ==================== */

    @GetMapping("/route/list/{systemId}")
    public ApiResponse<List<ApiRoute>> listRoutes(@PathVariable Long systemId) {
        return ApiResponse.ok(systemService.listRoutes(systemId));
    }

    @PostMapping("/route/add")
    public ApiResponse<ApiRoute> addRoute(@RequestBody ApiRoute route) {
        return ApiResponse.ok(systemService.addRoute(route));
    }

    @PutMapping("/route/update")
    public ApiResponse<ApiRoute> updateRoute(@RequestBody ApiRoute route) {
        return ApiResponse.ok(systemService.updateRoute(route));
    }

    @DeleteMapping("/route/delete/{id}")
    public ApiResponse<?> deleteRoute(@PathVariable Long id) {
        systemService.deleteRoute(id);
        return ApiResponse.ok();
    }
}
