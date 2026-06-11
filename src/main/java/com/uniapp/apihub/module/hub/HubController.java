package com.uniapp.apihub.module.hub;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.uniapp.apihub.common.ApiResponse;
import com.uniapp.apihub.security.CurrentUserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * API 中台统一入口。
 *
 * 前端只需要面对稳定的 /api/hub/{sysCode}/{operation} 路径；
 * 不同系统的 SDK、HTTP、签名、鉴权等细节由各自的 SystemAdapter 处理。
 */
@RestController
@RequestMapping("/api/hub")
@SaCheckLogin
@RequiredArgsConstructor
public class HubController {

    private final SystemAdapterRegistry adapterRegistry;
    private final CurrentUserContext currentUserContext;

    @PostMapping("/{sysCode}/{operation}")
    public ApiResponse<Object> execute(@PathVariable String sysCode,
                                       @PathVariable String operation,
                                       @RequestBody(required = false) Map<String, Object> payload) {
        currentUserContext.requireHubPermission(sysCode, operation);
        return ApiResponse.ok(adapterRegistry.execute(sysCode, operation, payload));
    }
}
