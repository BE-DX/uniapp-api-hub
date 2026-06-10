package com.uniapp.apihub.module.hub;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.uniapp.apihub.common.ApiResponse;
import com.uniapp.apihub.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @PostMapping("/{sysCode}/{operation}")
    public ApiResponse<Object> execute(@PathVariable String sysCode,
                                       @PathVariable String operation,
                                       @RequestBody(required = false) Map<String, Object> payload) {
        checkHubPermission(sysCode, operation);
        return ApiResponse.ok(adapterRegistry.execute(sysCode, operation, payload));
    }

    private void checkHubPermission(String sysCode, String operation) {
        List<String> permissions = StpUtil.getPermissionList();
        String exact = sysCode + ":" + operation;
        if (permissions.contains("*:*")
                || permissions.contains(exact)
                || permissions.contains(sysCode + ":*")
                || permissions.contains("*:" + operation)) {
            return;
        }
        throw new BusinessException(403, "无权访问中台能力: " + exact);
    }
}
