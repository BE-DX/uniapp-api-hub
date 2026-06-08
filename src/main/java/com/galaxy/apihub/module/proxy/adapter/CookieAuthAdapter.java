package com.galaxy.apihub.module.proxy.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.galaxy.apihub.module.system.entity.SystemConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Cookie认证适配器 — 适用于需要Session Cookie的下游系统
 *
 * authConfig格式: {"cookieName":"KDSVCSessionId", "cookieValue":"xxx"}
 */
@Slf4j
@Component
public class CookieAuthAdapter implements AuthAdapter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String authType() {
        return "COOKIE";
    }

    @Override
    @SuppressWarnings("unchecked")
    public void apply(Request.Builder requestBuilder, SystemConfig system) {
        try {
            Map<String, String> authConfig = objectMapper.readValue(system.getAuthConfig(), Map.class);
            String cookieName = authConfig.get("cookieName");
            String cookieValue = authConfig.get("cookieValue");
            if (cookieName != null && cookieValue != null) {
                requestBuilder.header("Cookie", cookieName + "=" + cookieValue);
            }
            log.debug("CookieAuth: system={}, cookieName={}", system.getSysCode(), cookieName);
        } catch (Exception e) {
            log.error("Cookie认证头注入失败: {}", e.getMessage());
        }
    }
}
