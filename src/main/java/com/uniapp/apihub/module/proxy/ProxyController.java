package com.uniapp.apihub.module.proxy;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniapp.apihub.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 统一代理入口 — 所有外部API请求通过此控制器转发到下游系统
 */
@Slf4j
@RestController
@RequestMapping("/api/proxy")
@RequiredArgsConstructor
public class ProxyController {

    private final ProxyService proxyService;
    private final ObjectMapper objectMapper;

    /**
     * 通用代理转发 — 返回解析后的下游JSON
     */
    @SaCheckLogin
    @PostMapping("/{sysCode}/{routeKey}")
    public ApiResponse<Object> proxyPost(@PathVariable String sysCode,
                                          @PathVariable String routeKey,
                                          @RequestBody(required = false) String params) {
        try {
            String result = proxyService.execute(sysCode, routeKey, params);
            return ApiResponse.ok(objectMapper.readTree(result));
        } catch (Exception e) {
            log.error("代理响应解析失败, 返回原始字符串: {}", e.getMessage());
            // 如果下游返回的不是合法JSON，直接返回字符串
            return ApiResponse.ok(proxyService.execute(sysCode, routeKey, params));
        }
    }

    @SaCheckLogin
    @GetMapping("/{sysCode}/{routeKey}")
    public ApiResponse<Object> proxyGet(@PathVariable String sysCode,
                                         @PathVariable String routeKey,
                                         @RequestParam(required = false) String params) {
        try {
            String result = proxyService.execute(sysCode, routeKey, params != null ? params : "{}");
            return ApiResponse.ok(objectMapper.readTree(result));
        } catch (Exception e) {
            return ApiResponse.ok(proxyService.execute(sysCode, routeKey, params != null ? params : "{}"));
        }
    }
}
